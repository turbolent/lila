package lila.runtime;

public class LilaString extends LilaObject {

	static LilaClass lilaClass =
		new LilaClass("<string>", LilaString.class);

	String string;

	public LilaString(String string) {
		this.string = string;
	}

	@Override
	public Object getJavaValue() {
		return this.string;
	}

	@Override
	public String toString() {
		String escaped =
			StringEscapeUtils.escapeJava(this.string);
		return "\"" + escaped + "\"";
	}
}
