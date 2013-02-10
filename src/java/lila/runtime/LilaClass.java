package lila.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LilaClass extends LilaObject {

	static LilaClass lilaClass =
		new LilaClass(true, "<class>", LilaClass.class);

	private String name;
	private Class<?> javaClass;
	private boolean builtin;
	private LilaClass[] superclasses;
	private List<LilaClass> allSuperclasses;

	public LilaClass(boolean builtin, String name, Class<?> javaClass) {
		this(builtin, name, javaClass, null);
	}

	public LilaClass(boolean builtin, String name,
	          Class<?> javaClass, LilaClass[] superclasses)
	{
		super(lilaClass);
		this.builtin = builtin;
		this.name = name;
		this.javaClass = javaClass;
		if (superclasses == null)
			superclasses = new LilaClass[] { LilaObject.lilaClass };
		this.superclasses = superclasses;
		// workaround for mutual dependency between LilaClass and LilaObject
		if (javaClass == LilaClass.class) {
			// LilaObject.lilaClass still null, delay until LilaObject
			return;
		}
		if (javaClass == LilaObject.class) {
			LilaClass.lilaClass.superclasses = new LilaClass[] { this };
			LilaClass.lilaClass.allSuperclasses =
				Arrays.asList(new LilaClass[] { LilaClass.lilaClass, this });
		}
		this.allSuperclasses = C3.computeClassLinearization(this);
	}

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
		if (superclasses != null && superclasses.length == 0)
			superclasses = null;
		return new LilaClass(false, name, null, superclasses);
	}

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

	public boolean isSubtypeOf(LilaClass other) {
		if (other == this)
			return true;
		for (LilaClass superclass : this.superclasses) {
			if (superclass.isSubtypeOf(other))
				return true;
		}
		return false;
	}

	@Override
	public String toString() {
		String name = (this.name == null ? "" : " " + this.name);
		return "#[Class" + name + "]";
	}
}
