package lila.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MutableCallSite;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lila.runtime.dispatch.Method;
import lila.runtime.dispatch.Predicate;
import lila.runtime.dispatch.DispatchFunction;

public class LilaGenericFunction extends LilaCallable {

	public static final LilaClass lilaClass =
		new LilaClass(true, "<generic-function>", LilaGenericFunction.class,
		              LilaObject.lilaClass);

	public Map<Predicate, Method> methods = new HashMap<>();

	List<LilaObject> closedArguments = Collections.emptyList();

	public LilaGenericFunction() {
		super(lilaClass);
	}

	public void addMethod(Predicate predicate, MethodHandle handle) {
		Method method = new Method(handle);
		// generate identifier, for debugging purposes
		method.identifier = "m" + (methods.size() + 1);
		methods.put(predicate, method);
	}

	public DispatchFunction toDispatchFunction() {
		return DispatchFunction.fromMethods(this.methods);
	}

	@Override
	public LilaGenericFunction close(LilaObject value) {
		LilaGenericFunction gf = new LilaGenericFunction();
		gf.closedArguments = new ArrayList<LilaObject>();
		gf.closedArguments.addAll(this.closedArguments);
		gf.closedArguments.add(value);
		gf.methods = this.methods;
		return gf;
	}

	@Override
	public LilaObject apply(LilaObject[] arguments) {
		// TODO:
		return null;
	}

	@Override
	public String toString() {
		// TODO: show type signature
		return "#[GenericFunction]";
	}

	@Override
	LilaObject fallback
		(MutableCallSite callSite, LilaCallable callable, LilaObject[] args)
		throws Throwable
	{
		// TODO:
		System.out.println("GF CALL");
		return null;
	}

	public void dumpMethods() {
		StringBuilder builder = new StringBuilder();
		for (Entry<Predicate, Method> entry : this.methods.entrySet()) {
			builder.append(String.format("\n  when %s %s", entry.getKey(),
										 entry.getValue()));
		}
		System.err.println("GF" + builder);
	}
}
