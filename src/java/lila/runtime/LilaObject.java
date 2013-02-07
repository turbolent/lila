package lila.runtime;

abstract public class LilaObject {

	static LilaClass lilaClass =
		new LilaClass("<object>", LilaObject.class);

	public Object getJavaValue() {
		return this;
	}
	
	public boolean isTrue() {
		return true;
	}
}
