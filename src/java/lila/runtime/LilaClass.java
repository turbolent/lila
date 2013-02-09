package lila.runtime;

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
