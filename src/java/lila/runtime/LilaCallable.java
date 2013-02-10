package lila.runtime;

import java.lang.invoke.MutableCallSite;

abstract class LilaCallable extends LilaObject {

	public LilaCallable(LilaClass type) {
		super(type);
	}

	abstract LilaCallable close(LilaObject value);

	abstract LilaObject fallback
		(MutableCallSite callSite, LilaCallable callable, LilaObject[] args)
		throws Throwable;

	boolean hasRest = false;
}
