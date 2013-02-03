package lila.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class DynamicClassLoader extends ClassLoader {
	public Class<?> define(String name, byte[] classBytes) {
		return super.defineClass(name, classBytes, 0, classBytes.length);
	}

	public LilaObject run(Class<?> clazz, String name) throws Throwable {
		MethodHandle run = MethodHandles.lookup()
			.findStatic(clazz, name, MethodType.methodType(LilaObject.class));
		return (LilaObject)run.invokeExact();
	}
}
