package lila.runtime.dispatch;

import java.util.HashSet;
import java.util.Set;


////Classes

class Clazz {
	Clazz[] superclasses;
	String name;
	Set<Clazz> subclasses = new HashSet<>();

	static Set<Clazz> allClasses = new HashSet<>();

	Clazz(String name, Clazz... superclasses) {
		this.name = name;
		this.superclasses = superclasses;
		for (Clazz clazz : superclasses)
			clazz.addSubclass(this);
		subclasses.add(this);
		allClasses.add(this);
	}

	void addSubclass(Clazz clazz) {
		this.subclasses.add(clazz);
		for (Clazz superclass : this.superclasses)
			superclass.addSubclass(clazz);
	}

	boolean isInstance(Instance instance) {
		return instance.clazz.equals(this) ||
			this.subclasses.contains(instance.clazz);
	}

	Clazz negate() {
		return new NegatedClazz(this);
	}

	@Override
	public String toString() {
		return this.name;
	}
}

class BooleanClazz extends Clazz {
	static Clazz CLAZZ = new BooleanClazz();

	private BooleanClazz() {
		super("Boolean");
	}
}

class TrueClazz extends Clazz {
	static Clazz CLAZZ = new TrueClazz();

	private TrueClazz() {
		super("true", BooleanClazz.CLAZZ);
	}
}

class FalseClazz extends Clazz {
	static Clazz CLAZZ = new FalseClazz();

	private FalseClazz() {
		super("false", BooleanClazz.CLAZZ);
	}
}

class NegatedClazz extends Clazz {
	Clazz clazz;

	public NegatedClazz(Clazz clazz) {
		super(null);
		this.clazz = clazz;
	}

	@Override
	Clazz negate() {
		return this.clazz;
	}

	@Override
	public String toString() {
		return "!" + this.clazz.toString();
	}
}
