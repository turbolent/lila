package lila.runtime;

public class LilaFalse extends LilaBoolean {

	public static final LilaClass lilaClass; 
	static {
		lilaClass = new LilaClass(true, "<false>", LilaFalse.class,
		                          LilaBoolean.lilaClass);
		LilaClass.updateMultiMethods(lilaClass);
	}

	protected LilaFalse() {
		super(lilaClass, false);
	}
}
