package lila.runtime;

import java.util.Arrays;

public class LilaClass extends LilaObject {

	static LilaClass lilaClass =
		new LilaClass(true, "<class>", LilaClass.class);

	private String name;
	private Class<?> javaClass;
	private boolean builtin;
	private LilaClass[] superclasses;

	LilaClass(boolean builtin, String name, Class<?> javaClass) {
		this(builtin, name, javaClass, null);
	}

	LilaClass(boolean builtin, String name, Class<?> javaClass, LilaClass[] superclasses) {
		super(lilaClass);
		this.builtin = builtin;
		this.name = name;
		this.javaClass = javaClass;
		if (superclasses == null)
			superclasses = new LilaClass[] { LilaObject.lilaClass };
		this.superclasses = superclasses;
	}

	// wrapper, called from programs with lila objects
	public static LilaObject make(LilaObject[] arguments) {
		String name = (arguments.length > 0
						? ((LilaString)arguments[0]).string
						: null);
		// no casting
		LilaClass[] actualSuperclasses = null;
		if (arguments.length > 1) {
			LilaObject[] superclasses = ((LilaArray)arguments[1]).array;
			actualSuperclasses = Arrays.copyOf(superclasses, superclasses.length,
			                                   LilaClass[].class);
		}
		return make(name, actualSuperclasses);
	}

	// actual implementation, called internally with java objects
	public static LilaClass make(String name, LilaClass[] superclasses) {
		if (superclasses != null && superclasses.length == 0)
			superclasses = null;
		return new LilaClass(false, name, null, superclasses);
	}

	public boolean isBuiltin() {
		return this.builtin;
	}

	public String getName() {
		return this.name;
	}

	public Class<?> getJavaClass() {
		return this.javaClass;
	}

	@Override
	public String toString() {
		return "#[Class " + this.name + "]";
	}
}
