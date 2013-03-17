package lila.runtime;

import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

public class LilaCallSite extends MutableCallSite {

	int chainCount = 0;

	public LilaCallSite(MethodType type) {
		super(type);
	}
}
