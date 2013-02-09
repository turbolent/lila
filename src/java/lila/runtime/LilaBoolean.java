package lila.runtime;

public class LilaBoolean extends LilaObject {

	static LilaClass lilaClass =
		new LilaClass(true, "<boolean>", LilaBoolean.class);

	public static LilaBoolean TRUE = new LilaBoolean(true);
	public static LilaBoolean FALSE = new LilaBoolean(false);

	private boolean value;

	protected LilaBoolean(boolean value) {
		this.value = value;
	}

	public static LilaBoolean box(boolean value) {
		return value ? TRUE : FALSE;
	}

	@Override
	public Object getJavaValue() {
		return this.value;
	}

	@Override
	public boolean isTrue() {
		return this.value;
	}

	@Override
	public String toString() {
		return this.value + "";
	}
}
