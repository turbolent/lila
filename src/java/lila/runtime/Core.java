package lila.runtime;

public class Core {
	static LilaString print(LilaString string) {
		System.out.println(string.string);
		return string;
	}

	static LilaString asString(LilaObject value) {
		return new LilaString(value.toString());
	}

	static LilaInteger plus(LilaInteger a, LilaInteger b) {
		return new LilaInteger(a.value + b.value);
	}

	static LilaInteger minus(LilaInteger a, LilaInteger b) {
		return new LilaInteger(a.value - b.value);
	}

	static LilaBoolean lessThan(LilaInteger a, LilaInteger b) {
		return new LilaBoolean(a.value < b.value);
	}
}
