package lila.runtime.dispatch.multiple;

import java.lang.invoke.MethodHandle;

import lila.runtime.LilaClass;

public class Method implements Comparable<Method> {
	private LilaClass[] specializers;
	private MethodHandle methodHandle;
	private int arity;

	
	public Method(LilaClass[] specializers, MethodHandle methodHandle) {
		this.specializers = specializers;
		this.methodHandle = methodHandle;
		this.arity = specializers.length;
	}
	
	public MethodHandle getMethodHandle() {
		return methodHandle;
	}
	
	public LilaClass getSpecializer(int i) {
		return this.specializers[i];
	}
	
	@Override
	public int compareTo(Method other) {
		int state = 0;
		for (int i = 0; i < this.arity; i++) {
			LilaClass s1 = this.specializers[i];
			LilaClass s2 = other.specializers[i];
			int cmp = s1.compareTo(s2);
			if (cmp == 0 || cmp == state)
				continue;
			else if (state == 0)
				state = cmp;
		}
		return state;
	}	
}
