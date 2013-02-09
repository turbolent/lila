package lila.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodType.methodType;

public class LilaFunction extends LilaCallable {

	static LilaClass lilaClass =
		new LilaClass(true, "<function>", LilaFunction.class);

	private MethodHandle methodHandle;

	public LilaFunction(MethodHandle methodHandle) {
		super(lilaClass);
		this.methodHandle = methodHandle;
		// all parameters are required by default
		this.requiredParameterCount =
			methodHandle.type().parameterCount();
	}

	@Override
	public Object getJavaValue() {
		return methodHandle;
	}

	public LilaFunction close(LilaObject value) {
		return new LilaFunction(this.methodHandle.bindTo(value));
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

	@Override
	public LilaObject fallback
		(MutableCallSite callSite, LilaCallable callable, LilaObject[] args)
		throws Throwable
	{
		LilaFunction function = (LilaFunction)callable;
		MethodHandle mh = function.methodHandle;
		MethodType callSiteType = callSite.type();
		int actualCallSiteParameterCount = callSiteType.parameterCount() - 1;

		// variable argument function?
		// create adapter collecting additional arguments
		if (actualCallSiteParameterCount >= function.requiredParameterCount
			&& function.hasRest)
		{
			int count = (actualCallSiteParameterCount
						 - function.requiredParameterCount);
			mh = mh.asCollector(LilaObject[].class, count);
		}

		// adapter that drops first argument (function) and
		// calls the actual method handle
		MethodHandle target = MethodHandles
			.dropArguments(mh, 0, LilaObject.class)
			.asType(callSiteType);

		MethodHandle mhTest = CHECK_MH.bindTo(target);

		MethodType mhTestType = mhTest.type()
			.changeParameterType(0, callSiteType.parameterType(0));
		mhTest = mhTest.asType(mhTestType);

		// ATM not a cache, always changing target in this fallback
		// if method handle changes
		MethodHandle fallback = RT.FALLBACK.bindTo(callSite)
			// -1: function
			.asCollector(LilaObject[].class,
			             actualCallSiteParameterCount);

		MethodHandle guard =
			MethodHandles.guardWithTest(mhTest, target, fallback);
		callSite.setTarget(guard);
		return (LilaObject)mh.invokeWithArguments((Object[])args);
	}

	static final Lookup LOOKUP = MethodHandles.lookup();
	private static final MethodHandle CHECK_MH;
	static {
		try {
			MethodType checkMHType = methodType(boolean.class,
			                                    MethodHandle.class, LilaObject.class);
			CHECK_MH = LOOKUP.findStatic(LilaFunction.class, "checkMH", checkMHType);
	    } catch (ReflectiveOperationException e) {
	    	throw (AssertionError)new AssertionError().initCause(e);
	    }
	}
}
