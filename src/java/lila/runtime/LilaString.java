package lila.runtime;

public class LilaString extends LilaObject {

	static LilaClass lilaClass =
		new LilaClass(true, "<string>", LilaString.class);

	String string;

	public LilaString(String string) {
		super(lilaClass);
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
