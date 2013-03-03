package lila.runtime;

public class LilaInteger extends LilaObject {

	public static final LilaClass lilaClass =
		new LilaClass(true, "<integer>", LilaInteger.class,
		              LilaObject.lilaClass);

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
	public boolean equals(Object obj) {
		return (obj.getClass() == this.getClass()
            	&& ((LilaInteger)obj).value == this.value);
	}

	@Override
	public String toString() {
		return this.value + "";
	}
}
