package lila.runtime;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LilaClass
	extends LilaObject
	implements Comparable<LilaClass>
{

	// see static initialization in LilaObject
	public static LilaClass lilaClass;

	private static int classCount = 0;

	private int identifier;
	protected String name;
	private Class<?> javaClass;
	private boolean builtin;
	private LilaClass[] superclasses;
	private List<LilaClass> allSuperclasses;
	private Set<LilaClass> allSubclasses = new HashSet<>();
	String[] classProperties;


	public LilaClass(boolean builtin, String name,
        Class<?> javaClass, LilaClass... superclasses)
	{
		this(lilaClass, builtin, name, javaClass, superclasses);
	}

	protected LilaClass(LilaClass type, boolean builtin, String name,
        Class<?> javaClass, LilaClass... superclasses)
	{
		super(type);
		this.identifier = classCount++;
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

	// wrapper, called from lila programs (parameters are lila objects)
	public static LilaObject make(LilaObject[] arguments) {
		String name = (arguments.length > 0
						? (String)arguments[0].getJavaValue()
						: null);
		// no casting
		LilaClass[] actualSuperclasses = null;
		if (arguments.length > 1) {
			LilaObject[] superclasses = (LilaObject[])arguments[1].getJavaValue();
			actualSuperclasses =
				Arrays.copyOf(superclasses, superclasses.length,
				              LilaClass[].class);
		}
		String[] actualProperties = null;
		if (arguments.length > 2) {
			LilaObject[] properties = (LilaObject[])arguments[2].getJavaValue();
			actualProperties = new String[properties.length];
			for (int i = 0; i < properties.length; i++) {
				LilaObject property = properties[i];
				actualProperties[i] = (String)property.getJavaValue();
			}
		}
		return make(name, actualSuperclasses, actualProperties);
	}

	// actual implementation, called internally (parameters are Java objects)
	public static LilaClass make(String name, LilaClass[] superclasses, String[] properties) {
		if (superclasses == null || superclasses.length == 0)
			superclasses = new LilaClass[] { LilaObject.lilaClass };
		LilaClass result = new LilaClass(false, name, null, superclasses);
		result.classProperties = properties;
		updateMultiMethods(result);
		return result;
	}

	static void updateMultiMethods(LilaClass type) {
		// update all multi-methods
		for (LilaMultiMethod mm : LilaMultiMethod.getInstances())
			mm.addClass(type);
	}

	//// Getters

	public int getIdentifier() {
		return this.identifier;
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

	public Set<LilaClass> getAllSubclasses() {
		return this.allSubclasses;
	}

	public boolean isSubtypeOf(LilaClass other) {
		if (other == this)
			return true;
		for (LilaClass superclass : this.allSuperclasses)
			if (superclass == other)
				return true;
		return false;
	}

	public boolean isInstance(LilaObject object) {
		if (object.getType() == this)
			return true;
		for (LilaClass subclass : this.allSubclasses)
			if (subclass == object.getType())
				return true;
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

	@Override
	public int compareTo(LilaClass other) {
		if (this == other)
			return 0;
		else if (this.isSubtypeOf(other))
			return -1;
		else
			return 1;
	}

}
