package lila.runtime;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.Random;

public class Core {
	static LilaString print(LilaString string) {
		System.out.println(string.string);
		return string;
	}

	static LilaString asString(LilaObject value) {
		return new LilaString(value.toString());
	}

	static LilaInteger plus(LilaInteger a, LilaInteger b) {
		return new LilaInteger(a.value + b.value);
	}

	static LilaInteger minus(LilaInteger a, LilaInteger b) {
		return new LilaInteger(a.value - b.value);
	}

	static LilaBoolean lessThan(LilaInteger a, LilaInteger b) {
		return LilaBoolean.box(a.value < b.value);
	}

	static LilaObject randomArgument(LilaObject ignored, LilaObject[] rest) {
		Random random = new Random();
		return rest[random.nextInt(rest.length)];
	}

	 static final Lookup lookup = MethodHandles.lookup();
	 static final MethodType builtinMakeType =
		 MethodType.methodType(LilaObject.class, LilaObject[].class);

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
	}
}
