package lila.runtime;

public class Core {
	static LilaString print(LilaString string) {
		System.out.println(string.getValue());
		return string;
	}

	static LilaString asString(LilaObject value) {
		return new LilaString(value.toString());
	}

	static LilaInteger plus(LilaInteger a, LilaInteger b) {
		return new LilaInteger(a.getValue() + b.getValue());
	}

	static LilaInteger minus(LilaInteger a, LilaInteger b) {
		return new LilaInteger(a.getValue() - b.getValue());
	}

	static LilaBoolean lessThan(LilaInteger a, LilaInteger b) {
		return new LilaBoolean(a.getValue() < b.getValue());
	}
}
