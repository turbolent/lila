package lila.runtime;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

public class LilaFunction extends LilaCallable {

	public static final LilaClass lilaClass =
		new LilaClass(true, "<function>", LilaFunction.class,
		              LilaObject.lilaClass);

	private MethodHandle methodHandle;

	public LilaFunction(String name, MethodHandle methodHandle) {
		super(lilaClass, name);
		this.methodHandle = methodHandle;
	}

	@Override
	public Object getJavaValue() {
		return methodHandle;
	}

	@Override
	public LilaFunction close(LilaObject value) {
		MethodHandle boundMethodHandle =
			this.methodHandle.bindTo(value);
		LilaFunction function =
			new LilaFunction(this.name, boundMethodHandle);
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

	static final Lookup lookup = MethodHandles.lookup();

	static LilaFunction wrap
		(Class<?> clazz, String name, MethodType type, String functionName)
	{
		try {
			MethodHandle mh = lookup.findStatic(clazz, name, type);
			return new LilaFunction(functionName, mh);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String toString() {
		// TODO: show type signature
		return String.format("#[Function %s]", this.name);
	}

	// call

	public static boolean check(LilaFunction function, LilaObject fn) {
		return fn == function;
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
			int count = (argumentCount - requiredParameterCount);
			mh = mh.asCollector(LilaObject[].class, count);
		}
		return mh;
	}

	static final int maxCainCount = 2;

	@Override
	public LilaObject fallback
		(LilaCallSite callSite, LilaCallable callable, LilaObject[] args)
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

		MethodHandle mhTest = check.bindTo(function);

		MethodType mhTestType = mhTest.type()
			.changeParameterType(0, callSiteType.parameterType(0));
		mhTest = mhTest.asType(mhTestType);

		MethodHandle fallback;
		if (callSite.chainCount > maxCainCount) {
			fallback = RT.fallback.bindTo(callSite)
				// -1: function
				.asCollector(LilaObject[].class, argumentCount);
			callSite.chainCount = 0;
		} else {
			fallback = callSite.getTarget();
			callSite.chainCount += 1;
		}

		MethodHandle guard =
			MethodHandles.guardWithTest(mhTest, target, fallback);
		callSite.setTarget(guard);

		return (LilaObject)mh.invokeWithArguments((Object[])args);
	}

	private static final MethodHandle check;
	private static MethodHandle boxAsArray;
	static {
		try {
			check = lookup
				.findStatic(LilaFunction.class, "check",
				            methodType(boolean.class,
				                       LilaFunction.class, LilaObject.class));
			boxAsArray = lookup
				.findConstructor(LilaArray.class,
				                 methodType(void.class,
				                            LilaObject[].class));
	    } catch (ReflectiveOperationException e) {
	    	throw (AssertionError)new AssertionError().initCause(e);
	    }
	}
}
