package lila.runtime.dispatch.predicate;

import java.lang.invoke.MethodHandle;

public class Method {
	private MethodHandle handle;

	public Method(MethodHandle handle) {
		this.handle = handle;
	}

	public MethodHandle getHandle() {
		return handle;
	}

	// Debugging

	public String identifier;

	@Override
	public String toString() {
		return String.format("{%s}", (this.identifier == null
									  ? this.handle
									  : this.identifier));
	}
}
