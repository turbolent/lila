package lila.runtime.dispatch;

import java.lang.invoke.MethodHandle;

public class Method {
	MethodHandle handle;

	Method(MethodHandle handle) {
		this.handle = handle;
	}

	// Debugging

	String identifier;

	@Override
	public String toString() {
		return String.format("{%s}", this.identifier);
	}
}