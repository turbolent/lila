package lila.runtime;

import java.lang.invoke.MethodHandle;

public class ExpressionInfo {
	private String className;
	String methodName;
	MethodHandle methodHandle;

	public ExpressionInfo
		(String className, String methodName, MethodHandle methodHandle)
	{
		this.className = className;
		this.methodName = methodName;
		this.methodHandle = methodHandle;
	}

	public String getClassName() {
		return className;
	}

	public String getMethodName() {
		return methodName;
	}

	public MethodHandle getMethodHandle() {
		return methodHandle;
	}

}
