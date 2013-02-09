package lila.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MutableCallSite;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LilaGenericFunction extends LilaCallable {

	static LilaClass lilaClass =
		new LilaClass(true, "<generic-function>", LilaGenericFunction.class);

	// specializers => method handle
	Map<List<LilaClass>, MethodHandle> methods = new HashMap<>();

	List<LilaObject> closedArguments =
		Collections.<LilaObject>emptyList();

	public LilaGenericFunction() {
		super(lilaClass);
	}

	public LilaGenericFunction close(LilaObject value) {
		LilaGenericFunction gf = new LilaGenericFunction();
		gf.closedArguments = new ArrayList<LilaObject>();
		gf.closedArguments.addAll(this.closedArguments);
		gf.closedArguments.add(value);
		gf.methods = this.methods;
		return gf;
	}

	public void addMethod(MethodHandle methodHandle,
						  List<LilaClass> specializers)
	{
		methods.put(specializers, methodHandle);
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

}
