package lila.runtime;


public class LilaNegatedClass extends LilaClass {

	public static final LilaClass lilaClass =
		new LilaClass(true, "<negated-class>", LilaNegatedClass.class,
		              LilaClass.lilaClass);

	LilaClass value;

	public LilaNegatedClass(LilaClass value) {
		// TODO: superclasses?
		super(lilaClass, false, "not-" + value.getName(), null);
		this.value = value;
	}

	@Override
	public String toString() {
		String name = (this.name == null ? "" : " " + this.name);
		return "#[NegatedClass" + name + "]";
	}

	@Override
	public LilaClass negate() {
		return this.value;
	}
}
