package lila.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.MethodHandles.Lookup;

public class LilaFunction extends LilaCallable {

	static LilaClass lilaClass =
		new LilaClass("<function>", LilaFunction.class);

	private MethodHandle methodHandle;

	public LilaFunction(MethodHandle value) {
		this.methodHandle = value;
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
		MethodHandle mh = ((LilaFunction)callable).methodHandle;
		MethodType type = callSite.type();

		// adapter that drops first argument (function) and
		// calls the actual method handle
		MethodHandle target =
			MethodHandles.dropArguments(mh, 0, LilaObject.class)
			.asType(type);

		MethodHandle mhTest = CHECK_MH.bindTo(target);

		MethodType mhTestType = mhTest.type()
			.changeParameterType(0, type.parameterType(0));
		mhTest = mhTest.asType(mhTestType);

		// ATM not a cache, always changing target in this fallback
		// if method handle changes
		MethodHandle fallback = RT.FALLBACK.bindTo(callSite)
			// -1: function
			.asCollector(LilaObject[].class, type.parameterCount() - 1);

		MethodHandle guard =
			MethodHandles.guardWithTest(mhTest, target, fallback);
		callSite.setTarget(guard);
		return (LilaObject)mh.invokeWithArguments((Object[])args);
	}

	static final Lookup LOOKUP = MethodHandles.lookup();
	private static final MethodHandle CHECK_MH;
	static {
		try {
			MethodType checkMHType =
				MethodType.methodType(boolean.class,
				                      MethodHandle.class, LilaObject.class);
			CHECK_MH = LOOKUP.findStatic(LilaFunction.class, "checkMH", checkMHType);
	    } catch (ReflectiveOperationException e) {
	    	throw (AssertionError)new AssertionError().initCause(e);
	    }
	}
}
