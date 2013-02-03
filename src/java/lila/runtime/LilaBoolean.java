package lila.runtime;

public class LilaBoolean extends LilaObject {
	private boolean value;

	public LilaBoolean(boolean value) {
		this.value = value;
	}

	public boolean getValue() {
		return value;
	}

	@Override
	public boolean isTrue() {
		return value;
	}

	@Override
	public String toString() {
		return value + "";
	}

	static String getLilaName() {
		return "<boolean>";
	}
}
