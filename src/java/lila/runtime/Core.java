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

	static LilaObject randomArgument(LilaObject ignored, LilaArray rest) {
		Random random = new Random();
		LilaObject[] objects = rest.array;
		return objects[random.nextInt(objects.length)];
	}

	static {
		LilaFunction randomArgument =
			exposeCoreFunction("random-argument", "randomArgument",
			                   methodType(LilaObject.class,
			                              LilaObject.class, LilaArray.class));
		randomArgument.hasRest = true;
	}

	// make

	static final MethodType builtinMakeType =
		methodType(LilaObject.class,
		           LilaObject[].class);

	static LilaObject make(LilaClass lilaClass, LilaArray rest)
		throws Throwable
	{
		Class<?> javaClass = lilaClass.getJavaClass();
		LilaObject object = null;
		if (lilaClass.isBuiltin()) {
			MethodHandle mh = lookup
				.findStatic(javaClass, "make", builtinMakeType);
			object = (LilaObject)mh.invokeExact(rest.array);
		} else {
			object = new LilaObject(lilaClass);
		}
		return object;
	}

	static {
		LilaFunction make =
			exposeCoreFunction("make",
			                   methodType(LilaObject.class,
			                              LilaClass.class, LilaArray.class));
		make.hasRest = true;
	}


	// object-class

	static LilaClass objectClass(LilaObject object) {
		return object.getType();
	}

	static {
		exposeCoreFunction("object-class", "objectClass",
		                   methodType(LilaClass.class,
		                              LilaObject.class));
	}


	// subtype?

	static LilaBoolean isSubtypeOf(LilaClass a, LilaClass b) {
		return LilaBoolean.box(a.isSubtypeOf(b));
	}

	static {
		exposeCoreFunction("subtype?", "isSubtypeOf",
		                   methodType(LilaBoolean.class,
		                              LilaClass.class, LilaClass.class));
	}


	// make-array

	static LilaArray makeArray(LilaArray rest) {
		return rest;
	}

	static {
		LilaFunction makeArray =
			exposeCoreFunction("make-array", "makeArray",
			                   methodType(LilaArray.class,
			                              LilaArray.class));
		makeArray.hasRest = true;
	}

	// ==

	static LilaBoolean equals(LilaObject object, LilaObject other) {
		return new LilaBoolean(object.equals(other));
	}

	static {
		exposeCoreFunction("==", "equals",
		                   methodType(LilaBoolean.class,
		                              LilaObject.class, LilaObject.class));
	}

	// apply

	// TODO: make generic for LilaCallable
	static LilaObject apply(LilaFunction function, LilaArray arguments) {
		return function.apply(arguments.array);
	}

	static {
		exposeCoreFunction("apply",
		                   methodType(LilaObject.class,
		                              LilaFunction.class, LilaArray.class));
	}
}
