package lila.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import lila.runtime.dispatch.multiple.Method;
import lila.runtime.dispatch.multiple.SRPDispatcher;

import static java.lang.invoke.MethodType.methodType;

public class LilaMultiMethod extends LilaCallable {

	public static final LilaClass lilaClass =
		new LilaClass(true, "<multi-method>", LilaMultiMethod.class,
		              LilaObject.lilaClass);
	
	private static List<LilaMultiMethod> instances = new LinkedList<>();

	private int arity;
	private List<LilaObject> closedArguments = Collections.emptyList();
	private SRPDispatcher dispatcher;

	
	public LilaMultiMethod(String name, int arity) {
		super(lilaClass, name);
		this.arity = arity;
		this.dispatcher = new SRPDispatcher(arity);
		instances.add(this);
	}

	public static List<LilaMultiMethod> getInstances() {
		return instances;
	}
	
	public int getArity() {
		return this.arity;
	}

 	public void addMethod(LilaClass[] specializers, MethodHandle methodHandle) {
 		Method method = new Method(specializers, methodHandle);
 		this.dispatcher.addNewMethod(method);
	}
 	
 	private LilaClass[] types(LilaObject[] args) {
 		LilaClass[] types = new LilaClass[this.arity];
		for (int i = 0; i < this.arity; i++)
			types[i] = args[i].getType();	
		return types;
 	}
 	
	@Override
	public LilaMultiMethod close(LilaObject value) {
		LilaMultiMethod mm = new LilaMultiMethod(this.name, this.arity);
		mm.closedArguments = new ArrayList<LilaObject>();
		mm.closedArguments.addAll(this.closedArguments);
		mm.closedArguments.add(value);
		// TODO: copy methods
		return mm;
	}

	@Override
	public LilaObject apply(LilaObject[] arguments) {
		try {
			LilaClass[] types = types(arguments);
			MethodHandle[] methods = this.dispatcher.dispatch(types);
			return (LilaObject)methodHandleForArguments(this, methods[0], arguments.length)
					.invokeWithArguments((Object[])arguments);
		} catch (Throwable e) {
			return null;
		}
	}

	@Override
	public String toString() {
		return String.format("#[MultiMethod %s]", this.getName());
	}
	
	private MethodHandle methodHandleForArguments
		(LilaMultiMethod mm, MethodHandle handle, int argumentCount)
	{
		int requiredParameterCount = mm.arity;
		// variadic and additional arguments supplied?
		if (mm.isVariadic()
			&& argumentCount >= requiredParameterCount)
		{
			// create adapter boxing the additional arguments array
			int pos = requiredParameterCount;
			handle = MethodHandles.filterArguments(handle, pos, RT.boxAsArray);
			// create adapter collecting additional arguments
			int count = (argumentCount - requiredParameterCount);
			handle = handle.asCollector(LilaObject[].class, count);
		}
		return handle;
	}

	public static boolean check
		(LilaMultiMethod siteMM, LilaClass[] types, LilaMultiMethod mm, LilaObject... args)
	{
		if (mm != siteMM)
			return false;
		for (int i = 0; i < types.length; i++) {
			if (types[i] != args[i].getType())
				return false;
		}
		return true;
	}

	// polymorphic inline cache chain limit
	static final int maxCainCount = 5;
	

	
	@Override
	LilaObject fallback
		(LilaCallSite callSite, LilaCallable callable, LilaObject[] args)
		throws Throwable
	{
		LilaMultiMethod mm = (LilaMultiMethod)callable;

		MethodType callSiteType = callSite.type();
		
		LilaClass[] types = types(args); 
		MethodHandle[] methods = mm.dispatcher.dispatch(types);
		
		// TODO: next-method
		MethodHandle mh = methods[0];
		
		int argumentCount = callSiteType.parameterCount() - 1;
		mh = methodHandleForArguments(mm, mh, argumentCount);
		
		// real target drops function
		MethodHandle target = MethodHandles
			.dropArguments(mh, 0, Object.class)
			.asType(callSiteType);
			
		MethodHandle mhTest = check
			.bindTo(mm)
			.bindTo(types)
			.asCollector(LilaObject[].class, args.length);
	
		MethodType checkType = mhTest.type()
			.changeParameterType(0, callSiteType.parameterType(0));
		mhTest = mhTest.asType(checkType);

		MethodHandle fallback;
		// check if polymorphic inline cache chain limit is reached
		if (callSite.chainCount > maxCainCount) {
			// guard fallback is this default fallback
			fallback = RT.fallback.bindTo(callSite)
				.asCollector(LilaObject[].class, argumentCount)
				.asType(callSiteType);
			callSite.chainCount = 0;
		} else {
			// set guard fallback to call site's current target
			fallback = callSite.getTarget();
			callSite.chainCount += 1;
		}
		
		callSite.setTarget(MethodHandles.guardWithTest(mhTest, 
		                                               target,
		                                               fallback));
		
		return (LilaObject)mh.invokeWithArguments((Object[])args);
	}
	
	private static final MethodHandle check;
	static {
		Lookup lookup = MethodHandles.lookup();
		try {
			check =
				lookup.findStatic(LilaMultiMethod.class, "check", 
				                  methodType(boolean.class, 
				                             LilaMultiMethod.class, LilaClass[].class, 
				                             LilaMultiMethod.class, LilaObject[].class));
		} catch (ReflectiveOperationException e) {
			throw (AssertionError) new AssertionError().initCause(e);
		}
	}

	public void addClass(LilaClass type) {
		this.dispatcher.addNewClass(type);
	}
}
