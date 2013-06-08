package lila.runtime;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import lila.runtime.dispatch.multiple.Method;
import lila.runtime.dispatch.multiple.SRPDispatcher;

public class LilaMultiMethod extends LilaCallable {

	private static List<LilaMultiMethod> instances = new LinkedList<>();
	
	public static final LilaClass lilaClass;
	static {
		lilaClass =	new LilaClass(true, "<multi-method>", LilaMultiMethod.class,
		           	              LilaObject.lilaClass);
		LilaClass.updateMultiMethods(lilaClass);
	}
		


	private int arity;
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
	public LilaObject apply(LilaObject[] arguments) {
		try {
			LilaClass[] types = types(arguments);
			MethodHandle mh = targetHandle(this, arguments, types, arguments.length);
			return (LilaObject)mh.invokeWithArguments((Object[])arguments);
		} catch (Throwable e) {
			return null;
		}
	}


	private static LilaObject makeNextMethod
		(LilaMultiMethod mm, LinkedList<MethodHandle> nextMethods, LilaObject[] args)
	{
		if (nextMethods.size() > 0) {
			MethodHandle nextMethod = callNextMethod
				.bindTo(mm)
				.bindTo(nextMethods)
				.bindTo(args);
			LilaFunction fn = new LilaFunction(mm.name, nextMethod);
			fn.setVariadic(true);
			return fn;
		} else
			return LilaBoolean.FALSE;
	}

	public static LilaObject callNextMethod
		(LilaMultiMethod mm, LinkedList<MethodHandle> nextMethods,
		 LilaObject[] oldArgs, LilaArray args)
		throws Throwable
	{
	   MethodHandle mh = nextMethods.remove();
	   LilaObject[] newArgs = args.array;
	   LilaObject[] passedArgs = newArgs.length > 0 ? newArgs : oldArgs;
	   LilaObject nextMethod = makeNextMethod(mm, nextMethods, passedArgs);
	   mh = mh.bindTo(nextMethod);
	   mh = methodHandleForArguments(mm, mh, passedArgs.length);
	   return (LilaObject)mh.invokeWithArguments((Object[])passedArgs);
	}

	@Override
	public String toString() {
		return String.format("#[MultiMethod %s]", this.getName());
	}

	static MethodHandle methodHandleForArguments
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

	static MethodHandle targetHandle
		(LilaMultiMethod mm, LilaObject[] args,
	     LilaClass[] types, int argumentCount)
	{
		MethodHandle[] methods = mm.dispatcher.dispatch(types);
		LinkedList<MethodHandle> nextMethods = new LinkedList<>();
		for (int i = 1; i < methods.length; i++)
			nextMethods.add(methods[i]);
		LilaObject nextMethod = makeNextMethod(mm, nextMethods, args);
		MethodHandle mh = methods[0].bindTo(nextMethod);
		return methodHandleForArguments(mm, mh, argumentCount);
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

	private static final String arityExceptionMessage =
		"Multi-method %s requires %d arguments, but passed %d";

	@Override
	LilaObject fallback
		(LilaCallSite callSite, LilaCallable callable, LilaObject[] args)
		throws Throwable
	{
		LilaMultiMethod mm = (LilaMultiMethod)callable;

		if (args.length < mm.arity) {
			throw new RuntimeException(String.format(arityExceptionMessage,
			                                         mm.name, mm.arity, args.length));
		}

		MethodType callSiteType = callSite.type();

		LilaClass[] types = types(args);


		// TODO: handle not-applicable!

		int argumentCount = callSiteType.parameterCount() - 1;
		MethodHandle mh = targetHandle(mm, args, types, argumentCount);

		//drop multi-method
		MethodHandle target = MethodHandles
			.dropArguments(mh, 0, LilaCallable.class)
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
	private static final MethodHandle callNextMethod;
	static {
		Lookup lookup = MethodHandles.lookup();
		try {
			check =
				lookup.findStatic(LilaMultiMethod.class, "check",
				                  methodType(boolean.class,
				                             LilaMultiMethod.class, LilaClass[].class,
				                             LilaMultiMethod.class, LilaObject[].class));
			callNextMethod =
				lookup.findStatic(LilaMultiMethod.class, "callNextMethod",
				                  methodType(LilaObject.class,
				                             LilaMultiMethod.class, LinkedList.class,
				                             LilaObject[].class, LilaArray.class));
		} catch (ReflectiveOperationException e) {
			throw (AssertionError) new AssertionError().initCause(e);
		}
	}

	public void addClass(LilaClass type) {
		this.dispatcher.addNewClass(type);
	}
}
