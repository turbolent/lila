package lila.runtime;

abstract class LilaCallable extends LilaObject {

	protected String name;

	public LilaCallable(LilaClass type, String name) {
		super(type);
		this.name = name;
	}

	abstract LilaCallable close(LilaObject value);

	abstract LilaObject apply(LilaObject[] arguments);

	abstract LilaObject fallback
		(LilaCallSite callSite, LilaCallable callable, LilaObject[] args)
		throws Throwable;

	boolean hasRest = false;
}
