package lila.runtime;

import java.util.HashMap;
import java.util.Map;

public class LilaObject {

	public static LilaClass lilaClass;
		
	static {
		lilaClass = new LilaClass(true, "<object>", LilaObject.class);
		LilaClass.updateMultiMethods(lilaClass);

		LilaClass.lilaClass = new LilaClass(true, "<class>", LilaClass.class,
		                                    LilaObject.lilaClass);
		LilaClass.updateMultiMethods(LilaClass.lilaClass);
	}

	private LilaClass type;

	private Map<String,LilaObject> properties = new HashMap<>();

	public LilaObject(LilaClass type) {
		this.type = type;
	}

	public Object getJavaValue() {
		return this;
	}

	public LilaClass getType() {
		return type;
	}

	public boolean isTrue() {
		return true;
	}

	public LilaObject getProperty(String propertyName) {
		LilaObject value = this.properties.get(propertyName);
		return value == null ? LilaBoolean.FALSE : value;
	}

	public void setProperty(String property, LilaObject value) {
		this.properties.put(property, value);
	}

	// wrapper, called from lila programs (parameters are lila objects)
	public static LilaObject make(LilaObject[] arguments) {
		return make();
	}

	// actual implementation, called internally (parameters are Java objects)
	public static LilaObject make() {
		return new LilaObject(lilaClass);
	}

	@Override
	public String toString() {
		return String.format("#[Object %s]", this.type);
	}
}
