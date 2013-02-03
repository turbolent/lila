package lila.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.Iterator;

public class LilaFunction extends LilaObject {
	private MethodHandle value;

	public LilaFunction(MethodHandle value) {
		this.value = value;
	}

	public MethodHandle getValue() {
		return this.value;
	}

	static final Lookup lookup = MethodHandles.lookup();
	static final MethodType lilaNameType = MethodType.methodType(String.class);

	static String getLilaNameForClass(Class<?> clazz) {
		String name = "?";
		try {
			name = (String)lookup
				.findStatic(clazz, "getLilaName", lilaNameType)
				.invokeExact();
		} catch (Throwable t) {}
		return name;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		Iterator<Class<?>> iter =
			this.value.type().parameterList().iterator();
		while (iter.hasNext()) {
			Class<?> clazz = iter.next();
			builder.append(getLilaNameForClass(clazz));
			if (!iter.hasNext())
				break;
			builder.append(", ");
		}
		return "#[Function (" + builder.toString() + ") => "
			+ getLilaNameForClass(this.value.type().returnType())
			+ "]";
	}

	public LilaFunction close(LilaObject value) {
		return new LilaFunction(this.value.bindTo(value));
	}

	static String getLilaName() {
		return "<function>";
	}
}
