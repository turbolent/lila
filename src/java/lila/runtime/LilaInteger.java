package lila.runtime;

public class LilaInteger extends LilaObject {

	static LilaClass lilaClass =
		new LilaClass(true, "<integer>", LilaInteger.class);

	public int value;

	public LilaInteger(int value) {
		super(lilaClass);
		this.value = value;
	}

	@Override
	public Object getJavaValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return this.value + "";
	}
}
