package lila.runtime;

public class LilaTrue extends LilaBoolean {

	public static final LilaClass lilaClass;
	static {
		lilaClass = new LilaClass(true, "<true>", LilaTrue.class,
		                          LilaBoolean.lilaClass);
		LilaClass.updateMultiMethods(lilaClass);
	}

	protected LilaTrue() {
		super(lilaClass, true);
	}
}
