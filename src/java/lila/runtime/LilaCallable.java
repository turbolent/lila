package lila.runtime;

abstract class LilaCallable extends LilaObject {

	protected String name;
	private boolean variadic = false;

	public LilaCallable(LilaClass type, String name) {
		super(type);
		this.name = name;
	}
	public boolean isVariadic() {
		return variadic;
	}

	public void setVariadic(boolean variadic) {
		this.variadic = variadic;
	}

	public String getName() {
		return name;
	}

	abstract LilaCallable close(LilaObject value);

	abstract LilaObject apply(LilaObject[] arguments);

	abstract LilaObject fallback
		(LilaCallSite callSite, LilaCallable callable, LilaObject[] args)
		throws Throwable;

}
