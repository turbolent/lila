package lila.runtime;

public class LilaClass extends LilaObject {

	static LilaClass lilaClass =
		new LilaClass(true, "<class>", LilaClass.class);

	private String name;
	private Class<?> javaClass;
	private boolean builtin;

	LilaClass(boolean builtin, String name, Class<?> javaClass) {
		this.builtin = builtin;
		this.name = name;
		this.javaClass = javaClass;
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

	// set and implemented in ruby
	public static LilaClassGenerator generator;
}
