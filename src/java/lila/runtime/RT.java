package lila.runtime;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.lang.invoke.MethodType.methodType;

public class RT {

	static Lookup lookup = MethodHandles.lookup();

	protected static Map<String,LilaObject> ENV = new HashMap<>();

	static {
		ENV.put("*lila-version*", new LilaString("0.1"));

		// TODO: refactor
		ENV.put("<object>", LilaObject.lilaClass);
		ENV.put("<string>", LilaString.lilaClass);
		ENV.put("<integer>", LilaInteger.lilaClass);
		ENV.put("<boolean>", LilaBoolean.lilaClass);
		ENV.put("<function>", LilaFunction.lilaClass);
		ENV.put("<class>", LilaClass.lilaClass);
		ENV.put("<array>", LilaArray.lilaClass);
	}


	//// values

	static Map<String, List<MutableCallSite>> valueCallSites = new HashMap<>();
	static Map<String, MethodHandle> valueMethodHandles = new HashMap<>();

	private static MethodHandle createValueMethodHandle(String name) {
		LilaObject value = ENV.get(name);
		MethodHandle methodHandle =
			MethodHandles.constant(LilaObject.class, value);
		valueMethodHandles.put(name, methodHandle);
		return methodHandle;
	}

	private static MethodHandle getOrCreateValueMethodHandle(String name) {
		MethodHandle methodHandle = valueMethodHandles.get(name);
		if (methodHandle == null)
			methodHandle = createValueMethodHandle(name);
		return methodHandle;
	}

	public static CallSite bootstrapValue
		(Lookup lookup, String name, MethodType type)
	{
		name = StringNames.toSourceName(name);

		// get or create constant method handle:
		// reuse same constant method handle instead
		// of creating a new method handle for each call site
		MethodHandle methodHandle = getOrCreateValueMethodHandle(name);

		// create and record callsite
		MutableCallSite callSite = new MutableCallSite(methodHandle);
		List<MutableCallSite> callSites = valueCallSites.get(name);
		if (callSites == null)
			callSites = new ArrayList<>();
		callSites.add(callSite);
		valueCallSites.put(name, callSites);
		return callSite;
	}

	public static void setValue(String name, LilaObject value) {
		// update value
		ENV.put(name, value);

		// update call sites
		List<MutableCallSite> callSites = valueCallSites.get(name);
		if (callSites != null) {
			// create new method handle for updated value
			MethodHandle methodHandle = createValueMethodHandle(name);
			// update all call sites using this value
			for (MutableCallSite callSite : callSites)
				callSite.setTarget(methodHandle);
			MutableCallSite.syncAll(callSites.toArray(new MutableCallSite[]{}));
		}
	}

	//// functions

	static Map<String,MethodHandle> FUNCTIONS = new HashMap<>();

	public static void registerInternalFunction
		(Class<?> clazz, String name, Class<?> rtype, Class<?>... ptypes)
		throws Throwable
	{
		// NOTE: no name decoding required, as it is not encoded
		MethodType type = methodType(rtype, ptypes);
		FUNCTIONS.put(name, lookup.findStatic(clazz, name, type));
	}

	public static CallSite bootstrapFunction
		(Lookup lookup, String name, MethodType type)
	{
		name = StringNames.toSourceName(name);
		MethodHandle handle = FUNCTIONS.get(name);
		LilaFunction function = new LilaFunction(handle);
		MethodHandle valueHandle =
			MethodHandles.constant(LilaFunction.class, function);
		return new ConstantCallSite(valueHandle);
	}

	//// calls

	public static CallSite bootstrapCall
		(Lookup lookup, String name, MethodType type)
	{
		CallSite callSite = new MutableCallSite(type);
		MethodHandle fallback = FALLBACK.bindTo(callSite)
			// -1: function
			.asCollector(LilaObject[].class, type.parameterCount() - 1);
	    callSite.setTarget(fallback);
		return callSite;
	}

	// generic fallback handler for all calls
	public static LilaObject fallback
	    (MutableCallSite callSite, LilaObject object, LilaObject[] args)
		throws Throwable
    {
		if (object instanceof LilaCallable) {
			LilaCallable callable = (LilaCallable)object;
			return callable.fallback(callSite, callable, args);
		} else
			throw new RuntimeException("unable to call: " + object);
    }

	static final MethodHandle FALLBACK;
	static {
		try {
			MethodType fallbackType =
				methodType(LilaObject.class,
				                      MutableCallSite.class, LilaObject.class,
				                      LilaObject[].class);
			FALLBACK = lookup.findStatic(RT.class, "fallback", fallbackType);
	    } catch (ReflectiveOperationException e) {
	    	throw (AssertionError)new AssertionError().initCause(e);
	    }
	}


	//// generic functions

	public static LilaGenericFunction findOrCreateGenericFunction(String name) {
		LilaGenericFunction gf = (LilaGenericFunction)RT.ENV.get(name);
		if (gf == null) {
			gf = new LilaGenericFunction();
			// TODO: setValue required?
			RT.ENV.put(name, gf);
		}
		return gf;
	}
}
