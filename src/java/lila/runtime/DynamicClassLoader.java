package lila.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static java.lang.invoke.MethodType.methodType;

public class DynamicClassLoader extends ClassLoader {

	public static DynamicClassLoader INSTANCE = new DynamicClassLoader();

	public Class<?> define(String name, byte[] classBytes) {
		return super.defineClass(name, classBytes, 0, classBytes.length);
	}

	public MethodHandle findMethod
		(Class<?> clazz, String name, Class<?> rtype, Class<?>... ptypes)
		throws Throwable
	{
		return findMethod(clazz, name, methodType(rtype, ptypes));
	}

	public MethodHandle findMethod
		(Class<?> clazz, String name, MethodType type)
		throws Throwable
	{
		return MethodHandles.lookup()
			.findStatic(clazz, name, type);
	}

	public LilaObject run(Class<?> clazz) throws Throwable {
		MethodHandle run = findMethod(clazz, "run", LilaObject.class);
		return (LilaObject)run.invokeExact();
	}
}
