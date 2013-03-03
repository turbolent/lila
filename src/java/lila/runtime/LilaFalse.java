package lila.runtime;

public class LilaFalse extends LilaBoolean {

	public static final LilaClass lilaClass =
		new LilaClass(true, "<false>", LilaFalse.class,
		              LilaBoolean.lilaClass);

	protected LilaFalse() {
		super(lilaClass, false);
	}
}
