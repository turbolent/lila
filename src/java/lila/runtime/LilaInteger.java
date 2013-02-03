package lila.runtime;

public class LilaInteger extends LilaObject {
	private int value;

	public LilaInteger(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	@Override
	public String toString() {
		return this.value + "";
	}

	static String getLilaName() {
		return "<integer>";
	}
}
