package lila.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import static java.lang.invoke.MethodType.methodType;

public class DynamicClassLoader extends ClassLoader {
	public Class<?> define(String name, byte[] classBytes) {
		return super.defineClass(name, classBytes, 0, classBytes.length);
	}

	public MethodHandle findMethod
		(Class<?> clazz, String name, Class<?> rtype, Class<?>...ptypes)
		throws Throwable
	{
		return MethodHandles.lookup()
			.findStatic(clazz, name, methodType(rtype, ptypes));
	}

	public LilaObject run(Class<?> clazz) throws Throwable {
		MethodHandle run = findMethod(clazz, "run", LilaObject.class);
		return (LilaObject)run.invokeExact();
	}
}
