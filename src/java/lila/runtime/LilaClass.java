package lila.runtime;

public class LilaClass extends LilaObject {

	static LilaClass lilaClass =
		new LilaClass("<class>", LilaClass.class);

	private String name;
	private Class<?> javaClass;

	LilaClass(String name, Class<?> javaClass) {
		this.name = name;
		this.javaClass = javaClass;
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
