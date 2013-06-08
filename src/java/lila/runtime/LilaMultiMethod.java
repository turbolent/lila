package lila.runtime;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	
	public static boolean checkMethod(LilaMultiMethod mm, LilaObject[] args) {
		return args[0] == mm;
	}
	
	public static boolean checkType(LilaClass type, Integer pos, LilaObject[] args) {
		return args[pos].getType() == type;
	}
	
	private static MethodHandle getFallback(LilaCallSite callSite, int argumentCount) {
		return RT.fallback.bindTo(callSite)
			.asCollector(LilaObject[].class, argumentCount - 1)
			.asSpreader(LilaObject[].class, argumentCount);
	}
	
	private static MethodHandle compileLevel0
		(LilaCallSite callSite, int argumentCount, LinkedList<LilaMultiMethod> remaining) 
	{
		if (remaining.size() == 0) {
			return getFallback(callSite, argumentCount);
		} else {
			LilaMultiMethod mm = remaining.remove();
			Map entry = callSite.multiMethodCache.get(mm);
			LinkedList<LilaClass> types = new LinkedList<>(); 
			types.addAll(entry.keySet());
			return MethodHandles.guardWithTest(checkMethod.bindTo(mm),
			                                   compileLevelN(1, callSite, argumentCount, entry, types),
			                                   compileLevel0(callSite, argumentCount, remaining));
		}
	}
	
	private static MethodHandle compileLevelN
		(int level, LilaCallSite callSite, int argumentCount, 
		 Map entry, LinkedList<LilaClass> remaining)
	{
		if (remaining.size() == 0) {
			return getFallback(callSite, argumentCount);
		} else {
			LilaClass type = remaining.remove();
			MethodHandle target;
			if (level + 1 == argumentCount) {
				MethodHandle mh = (MethodHandle)entry.get(type);
				target = mh.asSpreader(LilaObject[].class, argumentCount);	
			} else {
				Map nextEntry = (Map)entry.get(type);
				LinkedList<LilaClass> types = new LinkedList<>(); 
				types.addAll(nextEntry.keySet());
				target = compileLevelN(level + 1, callSite, argumentCount, nextEntry, types);	
			}
			return MethodHandles.guardWithTest(checkType.bindTo(type).bindTo(level),
			                                   target,
			                                   compileLevelN(level, callSite, argumentCount, 
			                                                 entry, remaining));
		}
	}

	private static MethodHandle compile(LilaCallSite callSite, int argumentCount) {
		LinkedList<LilaMultiMethod> methods = new LinkedList<>(); 
		methods.addAll(callSite.multiMethodCache.keySet());
		return compileLevel0(callSite, argumentCount, methods)
			.asCollector(LilaObject[].class, callSite.type().parameterCount());
	}

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

		// drop multi-method
		MethodHandle target = MethodHandles
			.dropArguments(mh, 0, LilaCallable.class)
			.asType(callSiteType);
		
		callSite.cache(mm, types, target);
		MethodHandle cache = compile(callSite, callSiteType.parameterCount());
		callSite.setTarget(cache);

		return (LilaObject)mh.invokeWithArguments((Object[])args);
	}

	private static final MethodHandle checkMethod;
	private static final MethodHandle checkType;
	private static final MethodHandle callNextMethod;
	static {
		Lookup lookup = MethodHandles.lookup();
		try {
			checkMethod =
				lookup.findStatic(LilaMultiMethod.class, "checkMethod",
				                  methodType(boolean.class,
				                             LilaMultiMethod.class, LilaObject[].class));
			checkType =
				lookup.findStatic(LilaMultiMethod.class, "checkType",
				                  methodType(boolean.class,
				                             LilaClass.class, Integer.class, 
				                             LilaObject[].class));
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
