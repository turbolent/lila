package lila.runtime;

public class LilaTrue extends LilaBoolean {

	public static final LilaClass lilaClass =
		new LilaClass(true, "<true>", LilaTrue.class,
		              LilaBoolean.lilaClass);

	protected LilaTrue() {
		super(lilaClass, true);
	}
}
