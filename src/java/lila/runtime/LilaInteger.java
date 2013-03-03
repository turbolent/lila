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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + value;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LilaInteger other = (LilaInteger) obj;
		if (value != other.value)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return this.value + "";
	}
}
