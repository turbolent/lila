package lila.runtime;

public class LilaString extends LilaObject {
	private String value;

	public LilaString(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		String escaped =
			StringEscapeUtils.escapeJava(this.value);
		return "\"" + escaped + "\"";
	}

	static String getLilaName() {
		return "<string>";
	}
}
