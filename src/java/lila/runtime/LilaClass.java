package lila.runtime;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LilaClass extends LilaObject {

	// see static initialization in LilaObject
	public static LilaClass lilaClass;

	protected String name;
	private Class<?> javaClass;
	private boolean builtin;
	private LilaClass[] superclasses;
	private List<LilaClass> allSuperclasses;
	private Set<LilaClass> allSubclasses = new HashSet<>();


	public LilaClass(boolean builtin, String name,
        Class<?> javaClass, LilaClass... superclasses)
	{
		this(lilaClass, builtin, name, javaClass, superclasses);
	}

	protected LilaClass(LilaClass type, boolean builtin, String name,
        Class<?> javaClass, LilaClass... superclasses)
	{
		super(type);
		this.builtin = builtin;
		this.name = name;
		this.javaClass = javaClass;
		this.superclasses = superclasses;
		this.allSuperclasses = C3.computeClassLinearization(this);
		for (LilaClass superclass : this.allSuperclasses)
			superclass.allSubclasses.add(this);
		this.allSubclasses.add(this);
	}

	//// Instantiation

	// wrapper, called from programs with lila objects
	public static LilaObject make(LilaObject[] arguments) {
		String name = (arguments.length > 0
						? ((LilaString)arguments[0]).string
						: null);
		// no casting
		LilaClass[] actualSuperclasses = null;
		if (arguments.length > 1) {
			LilaObject[] superclasses = ((LilaArray)arguments[1]).array;
			actualSuperclasses =
				Arrays.copyOf(superclasses, superclasses.length,
				              LilaClass[].class);
		}
		return make(name, actualSuperclasses);
	}

	// actual implementation, called internally with java objects
	public static LilaClass make(String name, LilaClass[] superclasses) {
		if (superclasses == null || superclasses.length == 0)
			superclasses = new LilaClass[] { LilaObject.lilaClass };
		return new LilaClass(false, name, null, superclasses);
	}


	//// Getters

	public boolean isBuiltin() {
		return this.builtin;
	}

	public String getName() {
		return this.name;
	}

	public Class<?> getJavaClass() {
		return this.javaClass;
	}

	public LilaClass[] getDirectSuperclasses() {
		return this.superclasses;
	}

	public List<LilaClass> getAllSuperclasses() {
		return this.allSuperclasses;
	}

	public Set<LilaClass> getAllSubclasses() {
		return this.allSubclasses;
	}

	public boolean isSubtypeOf(LilaClass other) {
		if (other == this)
			return true;
		for (LilaClass superclass : this.allSuperclasses) {
			if (superclass == other)
				return true;
		}
		return false;
	}

	public LilaClass negate() {
		return new LilaNegatedClass(this);
	}

	@Override
	public String toString() {
		String name = (this.name == null ? "" : " " + this.name);
		return "#[Class" + name + "]";
	}
}
