package lila.runtime;

public class LilaObject {

	public static LilaClass lilaClass =
		new LilaClass(true, "<object>", LilaObject.class);

	static {
		LilaClass.lilaClass = new LilaClass(true, "<class>", LilaClass.class,
		                                    LilaObject.lilaClass);
	}

	private LilaClass type;

	public LilaObject(LilaClass type) {
		this.type = type;
	}

	public Object getJavaValue() {
		return this;
	}

	public LilaClass getType() {
		return type;
	}

	public boolean isTrue() {
		return true;
	}
}
