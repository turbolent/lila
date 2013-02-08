package lila.runtime;

public class LilaBoolean extends LilaObject {

	static LilaClass lilaClass =
		new LilaClass(true, "<boolean>", LilaBoolean.class);

	private boolean value;

	public LilaBoolean(boolean value) {
		this.value = value;
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
