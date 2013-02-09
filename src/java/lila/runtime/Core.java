package lila.runtime;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.Random;

public class Core {

	static final Lookup lookup = MethodHandles.lookup();

	static LilaFunction makeLilaFunction
		(Class<?> clazz, String name, MethodType type)
	{
		try {
			MethodHandle mh = lookup.findStatic(clazz, name, type);
			return new LilaFunction(mh);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	static LilaFunction exposeCoreFunction
		(String exportedName, String name, MethodType type)
	{
		LilaFunction function = makeLilaFunction(Core.class, name, type);
		RT.ENV.put(exportedName, function);
		return function;
	}

	static LilaFunction exposeCoreFunction(String name, MethodType type) {
		return exposeCoreFunction(name, name, type);
	}

	//// functions


	// not

	static LilaBoolean not(LilaObject object) {
		return LilaBoolean.box(!object.isTrue());
	}

	static {
		exposeCoreFunction("not", methodType(LilaBoolean.class,
		                                     LilaObject.class));
	}

	// assert

	static LilaBoolean assertTrue(LilaObject object) {
		if (!object.isTrue())
			throw new AssertionError();
		return LilaBoolean.TRUE;
	}

	static {
		exposeCoreFunction("assert", "assertTrue",
		                   methodType(LilaBoolean.class,
		                              LilaObject.class));
	}

	// print

	static LilaString print(LilaString string) {
		System.out.println(string.string);
		return string;
	}

	static {
		exposeCoreFunction("print", methodType(LilaString.class,
		                                       LilaString.class));
	}

	// as-string

	static LilaString asString(LilaObject value) {
		return new LilaString(value.toString());
	}

	static {
		exposeCoreFunction("as-string", "asString",
		                   methodType(LilaString.class,
		                              LilaObject.class));
	}


	// +

	static LilaInteger plus(LilaInteger a, LilaInteger b) {
		return new LilaInteger(a.value + b.value);
	}

	static {
		exposeCoreFunction("+", "plus",
		                   methodType(LilaInteger.class,
		                              LilaInteger.class, LilaInteger.class));
	}


	// -

	static LilaInteger minus(LilaInteger a, LilaInteger b) {
		return new LilaInteger(a.value - b.value);
	}

	static {
		exposeCoreFunction("-", "minus",
		                   methodType(LilaInteger.class,
		                              LilaInteger.class, LilaInteger.class));
	}

	// <

	static LilaBoolean lessThan(LilaInteger a, LilaInteger b) {
		return LilaBoolean.box(a.value < b.value);
	}

	static {
		exposeCoreFunction("<", "lessThan",
		                   methodType(LilaBoolean.class,
		                              LilaInteger.class, LilaInteger.class));
	}


	// random-argument

	static LilaObject randomArgument(LilaObject ignored, LilaObject[] rest) {
		Random random = new Random();
		return rest[random.nextInt(rest.length)];
	}

	 static final Lookup lookup = MethodHandles.lookup();
	 static final MethodType builtinMakeType =
		 MethodType.methodType(LilaObject.class, LilaObject[].class);
	static {
		LilaFunction randomArgument =
			exposeCoreFunction("random-argument", "randomArgument",
			                   methodType(LilaObject.class,
			                              LilaObject.class, LilaObject[].class));
		randomArgument.requiredParameterCount = 1;
		randomArgument.hasRest = true;
	}

	// make

	static LilaObject make(LilaClass lilaClass, LilaObject[] rest) throws Exception {
		if (lilaClass.isBuiltin()) {
			LilaObject object = null;
			try {
				object = (LilaObject)lookup
					.findStatic(lilaClass.getJavaClass(), "make", builtinMakeType)
					.invokeWithArguments((Object[])rest);
			} catch (Throwable t) {}
			return object;
		} else
			return (LilaObject)lilaClass.getJavaClass().newInstance();
	static {
		LilaFunction make =
			exposeCoreFunction("make",
			                   methodType(LilaObject.class,
			                              LilaClass.class, LilaObject[].class));
		make.requiredParameterCount = 1;
		make.hasRest = true;
	}

	}
}
