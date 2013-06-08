package lila.runtime;

import java.util.Arrays;

public class LilaArray extends LilaObject {

	public static final LilaClass lilaClass;
	static {
		lilaClass = new LilaClass(true, "<array>", LilaArray.class,
	                          LilaObject.lilaClass);
		LilaClass.updateMultiMethods(lilaClass);
	}

	LilaObject[] array;

	public LilaArray(LilaObject[] array) {
		super(lilaClass);
		this.array = array;
	}

	@Override
	public Object getJavaValue() {
		return this.array;
	}

	@Override
	public String toString() {
		return Arrays.deepToString(this.array);
	}
}
