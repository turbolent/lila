package lila.runtime.dispatch;

import java.lang.invoke.MethodHandle;

public class Method {
	MethodHandle handle;

	public Method(MethodHandle handle) {
		this.handle = handle;
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
