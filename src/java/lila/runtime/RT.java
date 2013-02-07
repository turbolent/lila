package lila.runtime;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.HashMap;
import java.util.Map;

public class RT {

	static final Lookup LOOKUP = MethodHandles.lookup();

	//// initialize environment

	public static Map<String,LilaObject> ENV = new HashMap<>();

	static void setFunction(String name, MethodType type) {
		setFunction(name, name, type);
	}

	static void setFunction(String exportedName, String name, MethodType type) {
		try {
			ENV.put(exportedName, new LilaFunction(LOOKUP.findStatic(Core.class, name, type)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static {
		setFunction("print", MethodType.methodType(LilaString.class,
		                                           LilaString.class));
		setFunction("as-string", "asString",
		            MethodType.methodType(LilaString.class,
		                                  LilaObject.class));
		setFunction("+", "plus",
		            MethodType.methodType(LilaInteger.class,
		                                  LilaInteger.class, LilaInteger.class));
		setFunction("-", "minus",
		            MethodType.methodType(LilaInteger.class,
		                                  LilaInteger.class, LilaInteger.class));
		setFunction("<", "lessThan",
		            MethodType.methodType(LilaBoolean.class,
		                                  LilaInteger.class, LilaInteger.class));
		ENV.put("*lila-version*", new LilaString("0.1"));
	}

	//// values

	public static CallSite bootstrapValue
		(Lookup lookup, String name, MethodType type)
	{
		name = StringNames.toSourceName(name);
		LilaObject value = ENV.get(name);
		MethodHandle valueHandle =
			MethodHandles.constant(LilaObject.class, value);
		return new ConstantCallSite(valueHandle);
	}

	//// functions

	static Map<String,MethodHandle> FUNCTIONS = new HashMap<>();

	public static void registerInternalFunction
		(Class<?> clazz, String name, Class<?> rtype, Class<?>... ptypes)
		throws Throwable
	{
		MethodType type = MethodType.methodType(rtype, ptypes);
		FUNCTIONS.put(name, LOOKUP.findStatic(clazz, name, type));
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
				MethodType.methodType(LilaObject.class,
				                      MutableCallSite.class, LilaObject.class,
				                      LilaObject[].class);
			FALLBACK = LOOKUP.findStatic(RT.class, "fallback", fallbackType);
	    } catch (ReflectiveOperationException e) {
	    	throw (AssertionError)new AssertionError().initCause(e);
	    }
	}


	//// generic functions

	public static LilaGenericFunction findOrCreateGenericFunction(String name) {
		LilaGenericFunction gf = (LilaGenericFunction)RT.ENV.get(name);
		if (gf == null) {
			gf = new LilaGenericFunction();
			RT.ENV.put(name, gf);
		}
		return gf;
	}
}
