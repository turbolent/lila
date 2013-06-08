package lila.runtime;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lila.runtime.dispatch.predicate.Case;
import lila.runtime.dispatch.predicate.LookupDAGBuilder;
import lila.runtime.dispatch.predicate.LookupDAGNode;
import lila.runtime.dispatch.predicate.Method;
import lila.runtime.dispatch.predicate.Predicate;

public class LilaPredicateMethod extends LilaCallable {

	public static final LilaClass lilaClass;
	static {
		lilaClass = new LilaClass(true, "<predicate-method>", LilaPredicateMethod.class,
		              LilaObject.lilaClass);
		LilaClass.updateMultiMethods(lilaClass);
	}

	// TODO:
	public static Method inapplicable = new Method(null);
	public static Method ambiguous = new Method(null);
	static {
		inapplicable.identifier = "INAPPLICABLE";
		ambiguous.identifier = "AMBIGUOUS";
	}


	private Map<Predicate,Case> cases = new HashMap<>();

	private List<Expression> inputExpressions = Collections.emptyList();

	private Map<Expression,ExpressionInfo> expressionInfo = new HashMap<>();

	private List<Method> methods;

	private int nextExpressionIdentifier = 1;

	public int arity;


	MethodHandle dispatcher;

	public LilaPredicateMethod(String name, int arity) {
		super(lilaClass, name);
		this.arity = arity;
		this.methods = new ArrayList<Method>() {{
			add(inapplicable);
			add(ambiguous);
		}};
	}

	public LilaPredicateMethod(String name, int arity, Expression... inputExpressions) {
		this(name, arity);
		// TODO: remove, debugging
		int cost = 0;
		for (Expression inputExpression : inputExpressions)
			inputExpression.cost = cost++;

		this.inputExpressions = Arrays.asList(inputExpressions);
	}

	public Collection<Case> getCases() {
		return this.cases.values();
	}

	public List<Expression> getInputExpressions() {
		return inputExpressions;
	}

	public List<Method> getMethods() {
		return methods;
	}

 	public void addMethodHandle(Predicate predicate, MethodHandle handle) {
		addMethod(predicate, new Method(handle));
 	}

 	public void addMethod(Predicate predicate, Method method) {
		for (Predicate conjunction : predicate.canonicalize()) {
			Case c = this.cases.get(conjunction);
			if (c == null) {
				c = new Case(conjunction);
				this.cases.put(conjunction, c);
			}
			c.methods.add(method);
		}
		if (!this.methods.contains(method))
			this.methods.add(method);
	}

	void compileExpressions(Compiler compiler) {
		for (Case c : this.cases.values()) {
			c.conjunction.compileExpressions(compiler);
		}
	};

	@Override
	public String toString() {
		return String.format("#[PredicateMethod %s]", this.getName());
	}

	public void dumpMethods() {
		StringBuilder builder = new StringBuilder();
		String sep = "  ";
		for (Case c : this.cases.values()) {
			builder.append(String.format("\n  %s %s => %s", sep,
			                             c.conjunction, c.methods));
			sep = "or";
		}
		System.err.println("GF" + builder);
	}

	public ExpressionInfo getExpressionInfo(Expression expression) {
		return this.expressionInfo.get(expression);
	}

	public void setExpressionInfo(Expression exp, ExpressionInfo expressionInfo) {
		this.expressionInfo.put(exp, expressionInfo);
	}

	public int getNextExpressionIdentifier() {
		return this.nextExpressionIdentifier++;
	}

	public void updateDispatcher() throws Throwable {
		LookupDAGBuilder builder = new LookupDAGBuilder(LilaObject.lilaClass.getAllSubclasses());
		LookupDAGNode node = builder.buildLookupDAG(this);
		builder.dump(node);
		this.dispatcher = LookupDAGBuilder.compile(node, this.arity)
			.bindTo(this);
	}

	@Override
	public LilaObject apply(LilaObject[] arguments) {
		try {
			MethodHandle mh = targetHandle(this, arguments.length);
			return (LilaObject)mh.invokeWithArguments((Object[])arguments);
		} catch (Throwable e) {
			return null;
		}
	}

	public static LilaObject invoke(LilaPredicateMethod pm, LilaObject[] args)
		throws Throwable
	{
		Method m = (Method)pm.dispatcher.invokeWithArguments((Object[])args);
		return (LilaObject)m.getHandle().invokeWithArguments((Object[])args);
	}

	public static boolean check(LilaPredicateMethod pm, LilaObject sitePM) {
		return sitePM == pm;
	}

	static MethodHandle methodHandleForArguments
		(LilaPredicateMethod pm, MethodHandle handle, int argumentCount)
	{
		int requiredParameterCount = pm.arity;
		if (pm.isVariadic())
			requiredParameterCount--;

		// pm variadic and additional arguments supplied?
		if (pm.isVariadic()
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

	static MethodHandle targetHandle(LilaPredicateMethod pm, int argumentCount) {
		MethodHandle mh = invoke.bindTo(pm)
			.asCollector(LilaObject[].class, argumentCount);
		return methodHandleForArguments(pm, mh, argumentCount);
	}

	// polymorphic inline cache chain limit
	static final int maxCainCount = 3;

	@Override
	public LilaObject fallback
		(LilaCallSite callSite, LilaCallable callable, LilaObject[] args)
		throws Throwable
	{

		LilaPredicateMethod pm = (LilaPredicateMethod)callable;

		MethodType callSiteType = callSite.type();
		int argumentCount = callSiteType.parameterCount() - 1;

		MethodHandle mh = targetHandle(pm, argumentCount);
		// drop predicate method
		MethodHandle target = MethodHandles
			.dropArguments(mh, 0, LilaCallable.class)
			.asType(callSiteType);

		MethodHandle mhTest = check.bindTo(pm);

		MethodType mhTestType = mhTest.type()
			.changeParameterType(0, callSiteType.parameterType(0));
		mhTest = mhTest.asType(mhTestType);

		MethodHandle fallback;
		// check if polymorphic inline cache chain limit is reached
		if (callSite.chainCount > maxCainCount) {
			// guard fallback is this default fallback
			fallback = RT.fallback.bindTo(callSite)
				// -1: function
				.asCollector(LilaObject[].class, argumentCount);
			callSite.chainCount = 0;
		} else {
			// set guard fallback to call site's current target
			fallback = callSite.getTarget();
			callSite.chainCount += 1;
		}

		MethodHandle guard =
			MethodHandles.guardWithTest(mhTest, target, fallback);
		callSite.setTarget(guard);

		return (LilaObject)mh.invokeWithArguments((Object[])args);
	}

	private static final MethodHandle check;
	private static final MethodHandle invoke;
	static {
		Lookup lookup = MethodHandles.lookup();
		try {
			check = lookup
				.findStatic(LilaPredicateMethod.class, "check",
				            methodType(boolean.class,
				                       LilaPredicateMethod.class, LilaObject.class));
			invoke = lookup
				.findStatic(LilaPredicateMethod.class, "invoke",
				            methodType(LilaObject.class,
				                       LilaPredicateMethod.class, LilaObject[].class));
		} catch (ReflectiveOperationException e) {
			throw (AssertionError)new AssertionError().initCause(e);
		}
	}
}
