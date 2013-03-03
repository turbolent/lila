package lila.runtime;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

public class LilaFunction extends LilaCallable {

	public static final LilaClass lilaClass =
		new LilaClass(true, "<function>", LilaFunction.class,
		              LilaObject.lilaClass);

	private MethodHandle methodHandle;

	public LilaFunction(MethodHandle methodHandle) {
		super(lilaClass);
		this.methodHandle = methodHandle;
	}

	@Override
	public Object getJavaValue() {
		return methodHandle;
	}

	@Override
	public LilaFunction close(LilaObject value) {
		LilaFunction function = new LilaFunction(this.methodHandle.bindTo(value));
		function.hasRest = this.hasRest;
		return function;
	}

	@Override
	public LilaObject apply(LilaObject[] arguments) {
		try {
			return (LilaObject)methodHandleForArguments(this, arguments.length)
					.invokeWithArguments((Object[])arguments);
		} catch (Throwable e) {
			return null;
		}
	}

	@Override
	public String toString() {
		// TODO: show type signature
		return "#[Function]";
	}

	// call

	public static boolean checkMH(MethodHandle target, LilaObject fn) {
		// TODO: cast may fail, right?
		MethodHandle mh = ((LilaFunction)fn).methodHandle;
		return mh == target;
	}

	private MethodHandle methodHandleForArguments
		(LilaFunction function, int argumentCount)
	{
		MethodHandle mh = function.methodHandle;
		int requiredParameterCount = mh.type().parameterCount();
		if (function.hasRest)
			requiredParameterCount--;

		// variable argument function?
		if (argumentCount >= requiredParameterCount && function.hasRest) {
			// create adapter boxing additional arguments array
			int pos = requiredParameterCount;
			mh = MethodHandles.filterArguments(mh, pos, boxAsArray);
			// create adapter collecting additional arguments
			int count = (argumentCount
						 - requiredParameterCount);
			mh = mh.asCollector(LilaObject[].class, count);
		}
		return mh;
	}



	@Override
	public LilaObject fallback
		(MutableCallSite callSite, LilaCallable callable, LilaObject[] args)
		throws Throwable
	{
		LilaFunction function = (LilaFunction)callable;
		MethodType callSiteType = callSite.type();
		int argumentCount = callSiteType.parameterCount() - 1;

		MethodHandle mh = methodHandleForArguments(function, argumentCount);
		// adapter that drops first argument (function) and
		// calls the actual method handle
		MethodHandle target = MethodHandles
			.dropArguments(mh, 0, LilaObject.class)
			.asType(callSiteType);

		MethodHandle mhTest = checkMH.bindTo(target);

		MethodType mhTestType = mhTest.type()
			.changeParameterType(0, callSiteType.parameterType(0));
		mhTest = mhTest.asType(mhTestType);

		// ATM not a cache, always changing target in this fallback
		// if method handle changes
		MethodHandle fallback = RT.fallback.bindTo(callSite)
			// -1: function
			.asCollector(LilaObject[].class,
			             argumentCount);

		MethodHandle guard =
			MethodHandles.guardWithTest(mhTest, target, fallback);
		callSite.setTarget(guard);

		return (LilaObject)mh.invokeWithArguments((Object[])args);
	}

	static final Lookup lookup = MethodHandles.lookup();
	private static final MethodHandle checkMH;
	private static MethodHandle boxAsArray;
	static {
		try {
			checkMH = lookup
				.findStatic(LilaFunction.class, "checkMH",
				            methodType(boolean.class,
				                       MethodHandle.class, LilaObject.class));
			boxAsArray = lookup
				.findConstructor(LilaArray.class,
				                 methodType(void.class,
				                            LilaObject[].class));
	    } catch (ReflectiveOperationException e) {
	    	throw (AssertionError)new AssertionError().initCause(e);
	    }
	}
}
