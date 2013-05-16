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

	public static LilaObject getValue(String name) {
		return ENV.get(name);
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

	static Map<String,LilaFunction> FUNCTIONS = new HashMap<>();

	public static void registerInternalFunction
		(Class<?> clazz, String internalName, String name, boolean variadic,
		 Class<?> rtype, Class<?>... ptypes)
		throws Throwable
	{
		MethodType type = methodType(rtype, ptypes);
		// NOTE: no name decoding required, as it is not encoded
		MethodHandle handle = lookup.findStatic(clazz, internalName, type);
		LilaFunction function = new LilaFunction(name, handle);
		function.setVariadic(variadic);
		FUNCTIONS.put(internalName, function);
	}

	public static CallSite bootstrapFunction
		(Lookup lookup, String name, MethodType type)
	{
		name = StringNames.toSourceName(name);
		LilaFunction function = FUNCTIONS.get(name);
		MethodHandle valueHandle =
			MethodHandles.constant(LilaFunction.class, function);
		return new ConstantCallSite(valueHandle);
	}

	//// calls

	public static CallSite bootstrapCall
		(Lookup lookup, String name, MethodType type)
	{
		CallSite callSite = new LilaCallSite(type);
		MethodHandle target = fallback.bindTo(callSite)
			// -1: function
			.asCollector(LilaObject[].class, type.parameterCount() - 1);
	    callSite.setTarget(target);
		return callSite;
	}

	// generic fallback handler for all calls
	public static LilaObject fallback
	    (LilaCallSite callSite, LilaObject object, LilaObject[] args)
		throws Throwable
    {
		if (object instanceof LilaCallable) {
			LilaCallable callable = (LilaCallable)object;
			return callable.fallback(callSite, callable, args);
		} else
			throw new RuntimeException("unable to call: " + object);
    }

	static final MethodHandle fallback;
	static MethodHandle boxAsArray;
	static {
		try {
			MethodType fallbackType =
				methodType(LilaObject.class,
	                       LilaCallSite.class, LilaObject.class, LilaObject[].class);
			fallback = lookup.findStatic(RT.class, "fallback", fallbackType);
			boxAsArray = lookup
				.findConstructor(LilaArray.class,
				                 methodType(void.class, LilaObject[].class));
	    } catch (ReflectiveOperationException e) {
	    	throw (AssertionError)new AssertionError().initCause(e);
	    }
	}

	public static void initialize() {
		Core.initialize();
	}
}
