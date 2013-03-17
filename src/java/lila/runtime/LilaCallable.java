package lila.runtime;

import java.lang.invoke.MutableCallSite;

abstract class LilaCallable extends LilaObject {

	protected String name;

	public LilaCallable(LilaClass type, String name) {
		super(type);
		this.name = name;
	}

	abstract LilaCallable close(LilaObject value);

	abstract LilaObject apply(LilaObject[] arguments);

	abstract LilaObject fallback
		(MutableCallSite callSite, LilaCallable callable, LilaObject[] args)
		throws Throwable;

	boolean hasRest = false;
}
