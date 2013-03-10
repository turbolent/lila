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

	static LilaFunction exportFunction
		(String exportedName, String name, MethodType type)
	{
		LilaFunction function = makeLilaFunction(Core.class, name, type);
		RT.ENV.put(exportedName, function);
		return function;
	}

	static LilaFunction exportFunction(String name, MethodType type) {
		return exportFunction(name, name, type);
	}

	static void exportClass(LilaClass c) {
		System.out.println("export class: " + c);
		RT.ENV.put(c.name, c);
	}

	static void initialize() {
		RT.ENV.put("*lila-version*", new LilaString("0.1"));
		exportClass(LilaObject.lilaClass);
		exportClass(LilaClass.lilaClass);
		exportClass(LilaNegatedClass.lilaClass);
		exportClass(LilaBoolean.lilaClass);
		exportClass(LilaFalse.lilaClass);
		exportClass(LilaTrue.lilaClass);
		exportClass(LilaFunction.lilaClass);
		exportClass(LilaGenericFunction.lilaClass);
		exportClass(LilaInteger.lilaClass);
		exportClass(LilaString.lilaClass);
	}

	//// functions


	// not

	static LilaBoolean not(LilaObject object) {
		return LilaBoolean.box(!object.isTrue());
	}

	static {
		exportFunction("not", methodType(LilaBoolean.class,
		                                 LilaObject.class));

	}

	// assert

	static LilaBoolean assertTrue(LilaObject object) {
		if (!object.isTrue())
			throw new AssertionError();
		return LilaBoolean.TRUE;
	}

	static {
		exportFunction("assert", "assertTrue",
		               methodType(LilaBoolean.class,
		                          LilaObject.class));
	}

	// print

	static LilaString print(LilaString string) {
		System.out.println(string.string);
		return string;
	}

	static {
		exportFunction("print", methodType(LilaString.class,
		                                   LilaString.class));
	}

	// as-string

	static LilaString asString(LilaObject value) {
		return new LilaString(value.toString());
	}

	static {
		exportFunction("as-string", "asString",
		               methodType(LilaString.class,
		                          LilaObject.class));
	}


	// +

	static LilaInteger plus(LilaInteger a, LilaInteger b) {
		return new LilaInteger(a.value + b.value);
	}

	static {
		exportFunction("+", "plus",
		               methodType(LilaInteger.class,
		                          LilaInteger.class, LilaInteger.class));
	}


	// -

	static LilaInteger minus(LilaInteger a, LilaInteger b) {
		return new LilaInteger(a.value - b.value);
	}

	static {
		exportFunction("-", "minus",
		               methodType(LilaInteger.class,
		                          LilaInteger.class, LilaInteger.class));
	}

	// <

	static LilaBoolean lessThan(LilaInteger a, LilaInteger b) {
		return LilaBoolean.box(a.value < b.value);
	}

	static {
		exportFunction("<", "lessThan",
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
			exportFunction("random-argument", "randomArgument",
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
			exportFunction("make",
			               methodType(LilaObject.class,
			                          LilaClass.class, LilaArray.class));
		make.hasRest = true;
	}


	// object-class

	static LilaClass objectClass(LilaObject object) {
		return object.getType();
	}

	static {
		exportFunction("object-class", "objectClass",
		               methodType(LilaClass.class,
		                          LilaObject.class));
	}


	// subtype?

	static LilaBoolean isSubtypeOf(LilaClass a, LilaClass b) {
		return LilaBoolean.box(a.isSubtypeOf(b));
	}

	static {
		exportFunction("subtype?", "isSubtypeOf",
		               methodType(LilaBoolean.class,
		                          LilaClass.class, LilaClass.class));
	}


	// instance?

	static LilaBoolean isInstanceOf(LilaObject object, LilaClass type) {
		return LilaBoolean.box(type.isInstance(object));
	}

	static {
		exportFunction("instance?", "isInstanceOf",
		               methodType(LilaBoolean.class,
		                          LilaObject.class, LilaClass.class));
	}



	// make-array

	static LilaArray makeArray(LilaArray rest) {
		return rest;
	}

	static {
		LilaFunction makeArray =
			exportFunction("make-array", "makeArray",
			               methodType(LilaArray.class,
			                          LilaArray.class));
		makeArray.hasRest = true;
	}

	// ==

	static LilaBoolean equals(LilaObject object, LilaObject other) {
		return LilaBoolean.box(object.equals(other));
	}

	static {
		exportFunction("==", "equals",
		               methodType(LilaBoolean.class,
		                          LilaObject.class, LilaObject.class));
	}

	// apply

	// TODO: make generic for LilaCallable
	static LilaObject apply(LilaFunction function, LilaArray arguments) {
		return function.apply(arguments.array);
	}

	static {
		exportFunction("apply",
		               methodType(LilaObject.class,
		                          LilaFunction.class, LilaArray.class));
	}
}
