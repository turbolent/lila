package lila.runtime.dispatch.predicate;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import lila.runtime.Expression;
import lila.runtime.ExpressionEnvironment;
import lila.runtime.ExpressionInfo;
import lila.runtime.LilaClass;
import lila.runtime.LilaFalse;
import lila.runtime.LilaPredicateMethod;
import lila.runtime.LilaObject;
import lila.runtime.LilaTrue;
import lila.runtime.RT;

class Var extends Expression {
	String name;

	public Var(String identifier) {
		this.name = identifier;
	}

	@Override
	public LilaObject evaluate(ExpressionEnvironment env) {
		return env.get(this.name);
	}

	@Override
	public String toString() {
		return this.name;
	}

	public Expression resolveBindings(PredicateEnvironment env) {
		Expression subst = env.get(this.name);
		if (subst == null)
			return this;
		else
			return subst;
	}
}

class BinaryExpression extends Expression {
	String operation;
	Expression left;
	Expression right;

	BinaryExpression(String operation, Expression left, Expression right) {
		this.operation = operation;
		this.left = left;
		this.right = right;
	}

	@Override
	public LilaObject evaluate(ExpressionEnvironment env) {
		// TODO
		return null;
	}

	@Override
	public String toString() {
		return String.format("(%s %s %s)",
		                     this.left, this.operation, this.right);
	}

	@Override
	public Expression resolveBindings(PredicateEnvironment env) {
		this.left = this.left.resolveBindings(env);
		this.right = this.right.resolveBindings(env);
		return this;
	}
}

public class Test {

	public static void main(String[] args) throws Throwable {

		RT.initialize();



		// testDispatchTree();

		// test1();
		// System.out.println();
		// test2();

		test3();

	}





	private static void testDispatchTree() throws Throwable {
		LookupDAGInteriorNode node = new LookupDAGInteriorNode(null, null);

		int[] targets = new int[]{
			1, 2, 3, 1, 4, 2, 5, 6, 1, 7, 8, 8, 8, 8, 8, 8, 8, 9, 9, 9, 9, 8, 8, 8, 8,
			10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,
			11, 11, 11, 11, 11, 11, 11, 11, 11
		};

		int[] freqs = new int[]{
			6, 7, 7, 6, 8, 8, 7, 6, 8, 7, 10, 11, 10, 9, 500, 9, 10, 10, 8, 15, 17, 7, 8, 15,
			10, 1, 2, 2, 1, 2, 2, 100, 1, 2, 2, 2, 1, 1, 1, 2, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3
		};

		Map<Integer,Integer> frequencies = new HashMap<>();

		// generate classes and one unique target node for each target
		Map<Integer,LookupDAGLeafNode> targetNodes = new HashMap<>();
		for (int i = 0; i < targets.length; i++) {
			LilaClass c = new LilaClass(false, "c" + i, null);
			frequencies.put(c.getIdentifier(), freqs[i]);
			int target = targets[i];
			LookupDAGLeafNode targetNode = targetNodes.get(target);
			if (targetNode == null) {
				Method method = new Method(null);
				method.identifier = "n" + targets[i];
				targetNode = new LookupDAGLeafNode(null, method);
				targetNodes.put(target, targetNode);
			}
			node.edges.add(new LookupDAGEdge(c, targetNode));
		}

		DispatchTreeNode dispatchTree = new DispatchTreeBuilder()
			.buildDispatchTree(node, frequencies);

		System.err.println(dispatchTree);
		DispatchTreeBuilder.dump(dispatchTree);

		MethodHandle handle = dispatchTree.compileHandle();
		benchmarkHandle(handle, frequencies);

		MethodHandle handle2 =
			DispatchTreeBuilder.compileASMHandle(dispatchTree);
		benchmarkHandle(handle2, frequencies);
	}


	private static void benchmarkHandle
		(MethodHandle handle, Map<Integer,Integer> frequencies)
		throws Throwable
	{
		long startTime = System.currentTimeMillis();
		for (Entry<Integer,Integer> entry : frequencies.entrySet()) {
			int frequency = entry.getValue() * 10000;
			int identifier = entry.getKey();
			System.err.println(String.format("invoking %d %d times",
			                                 identifier, frequency));
			for (int j = 0; j < frequency; j++) {
				LookupDAGLeafNode targetNode =
					(LookupDAGLeafNode)(LookupDAGNode)handle.invokeExact(identifier);
				if (j == 0)
					System.err.println(targetNode.method.identifier);
			}
		}
		long endTime = System.currentTimeMillis();
		System.out.println("Time: " + (endTime-startTime) + "ms");
	}


	static void test1() throws Exception {
		final LilaClass clazzA = new LilaClass(false, "A", null);
		final LilaClass clazzB = new LilaClass(false, "B", null, clazzA);
		final LilaClass clazzC = new LilaClass(false, "C", null);
		final LilaClass clazzD = new LilaClass(false, "D", null, clazzA, clazzC);


		Set<LilaClass> allClasses = new HashSet<LilaClass>() {{
			add(clazzA);
			add(clazzB);
			add(clazzC);
			add(clazzD);
		}};

		Expression exp1 = new Var("f1");
		exp1.name = "e1";
		exp1.staticClasses = new HashSet<LilaClass>() {{
			add(clazzA);
			add(clazzB);
			add(clazzC);
		}};

		Expression exp2 = new Var("f1.x");
		exp2.name = "e3";
		exp2.staticClasses = allClasses;

		Expression exp3 = new Var("f2.x");
		exp3.name = "e5";
		exp3.staticClasses = new HashSet<LilaClass>() {{
			add(clazzC);
			add(clazzD);
		}};

		Expression exp4 = new BinaryExpression("=", new Var("f1.y"), new Var("f2.y"));
		exp4.name = "e4";
		exp4.staticClasses = new HashSet<LilaClass>() {{
			add(LilaTrue.lilaClass);
			add(LilaFalse.lilaClass);
		}};

		Expression exp5 = new Var("f2");
		exp5.name = "e2";
		exp5.staticClasses = allClasses;

		LilaPredicateMethod gf = new LilaPredicateMethod(null, 2, exp1, exp5);

		Predicate pred1 =
			AndPredicate.fromPredicates(new TypePredicate(exp1, clazzA),
			                            new BindingPredicate("t", exp2),
			                            new TypePredicate(new Var("t"), clazzA),
			                            new NotPredicate(new TypePredicate(new Var("t"), clazzB)),
			                            new TypePredicate(exp3, clazzC),
			                            new TestPredicate(exp4));
		Method m1 = new Method(null);
		m1.identifier = "m1";
		gf.addMethod(pred1, m1);

		Predicate pred2 =
			new AndPredicate(new TypePredicate(exp2, clazzB),
			                 new OrPredicate(new AndPredicate(new TypePredicate(exp1, clazzB),
			                                                  new TypePredicate(exp3, clazzC)),
			                                 new AndPredicate(new TypePredicate(exp1, clazzC),
			                                                  new TypePredicate(exp5, clazzA))));
		Method m2 = new Method(null);
		m2.identifier = "m2";
		gf.addMethod(pred2, m2);

		Predicate pred3 =
			new AndPredicate(new TypePredicate(exp1, clazzC),
			                 new TypePredicate(exp5, clazzC));
		Method m3 = new Method(null);
		m3.identifier = "m3";
		gf.addMethod(pred3, m3);

		Predicate pred4 =
			new TypePredicate(exp1, clazzC);
		Method m4 = new Method(null);
		m4.identifier = "m4";
		gf.addMethod(pred4, m4);

		gf.dumpMethods();


//		// Test DF => DAG
//		// (DF manually constructed, as example has
//		//  eliminated exp3 / e5, not performed by convert)
//
//		DispatchFunction df2 = new DispatchFunction(exp1, exp5);
//
//		// (f1@A and f1.x@A and f1.x@!B and (f1.y=f2.y)@true) => m1
//		Predicate p1 = AndPredicate.fromPredicates(new TypePredicate(exp1, clazzA),
//		                                           new TypePredicate(exp2, clazzA),
//		                                           new TypePredicate(exp2, new NegatedClazz(clazzB)),
//		                                           new TypePredicate(exp4, TrueClazz.CLAZZ));
//		df2.cases.put(p1, new HashSet<Method>() {{ add(new Method(1)); }});
//
//		final Method m2 = new Method(2);
//
//		// (f1.x@B and f1@B) => m2
//		Predicate p2 = AndPredicate.fromPredicates(new TypePredicate(exp2, clazzB),
//		                                           new TypePredicate(exp1, clazzB));
//		df2.cases.put(p2, new HashSet<Method>() {{ add(m2); }});
//
//		// (f1.x@B and f1@C and f2@A) => m2
//		Predicate p3 = AndPredicate.fromPredicates(new TypePredicate(exp2, clazzB),
//		                                           new TypePredicate(exp1, clazzC),
//		                                           new TypePredicate(exp5, clazzA));
//		df2.cases.put(p3, new HashSet<Method>() {{ add(m2); }});
//
//		// (f1@C and f2@C) => m3
//		Predicate p4 = AndPredicate.fromPredicates(new TypePredicate(exp1, clazzC),
//		                                           new TypePredicate(exp5, clazzC));
//		df2.cases.put(p4, new HashSet<Method>() {{ add(new Method(3)); }});
//
//		// (f1@C) => m4
//		Predicate p5 = new TypePredicate(exp1, clazzC);
//		df2.cases.put(p5, new HashSet<Method>() {{ add(new Method(4)); }});
//
//		System.out.println(df2);

		Set<LilaClass> classes = new HashSet<>();
		classes.addAll(allClasses);
		classes.add(LilaTrue.lilaClass);
		classes.add(LilaFalse.lilaClass);

		LookupDAGBuilder builder = new LookupDAGBuilder(classes);
		LookupDAGNode node = builder.buildLookupDAG(gf);
		System.out.println(node);
		builder.dump(node);
	}

	static void test2() throws Exception {

		//	type List;
		//	  class Cons subtypes List { head:Any, tail:List};
		//	  class Nil subtypes List;
		//
		//	signature Zip(List, List): List;
		//
		//	method Zip(lst1, lst2) when lst1@Cons and lst2@Cons {
		//	   return Cons(Pair(lst1.head, lst2.head),
		//	               Zip(lst1.tail, lst2.tail)); }
		//
		//	method Zip(lst1, lst2) when lst1@Nil or lst2@Nil { return Nil; }


		final LilaClass list = new LilaClass(false, "<list>", null, LilaObject.lilaClass);
		final LilaClass cons = new LilaClass(false, "<cons>", null, list);
		final LilaClass nil = new LilaClass(false, "<nil>", null, list);

		Set<LilaClass> staticClasses = LilaObject.lilaClass.getAllSubclasses();
		System.out.println("ALL: " + staticClasses);

		Expression exp1 = new Var("lst1");
		exp1.name = "e1";
//		exp1.staticClasses = new HashSet<LilaClass>() {{
//			add(cons);
//		}};

		Expression exp2 = new Var("lst2");
		exp2.name = "e2";
		//exp2.staticClasses = staticClasses;

		LilaPredicateMethod gf = new LilaPredicateMethod(null, 2);

		// method 1
		final Method m1 = new Method(null);
		m1.identifier = "Cons";
		Predicate pred1 =
			new AndPredicate(new TypePredicate(exp1, cons),
			                 new TypePredicate(exp2, cons));
		gf.addMethod(pred1, m1);

		// method 2
		final Method m2 = new Method(null);
		m2.identifier = "Nil";
		Predicate pred2 =
			new AndPredicate(new TypePredicate(exp1, nil),
			                 new TypePredicate(exp2, nil));
		gf.addMethod(pred2, m2);

		gf.dumpMethods();

		Set<LilaClass> classes = LilaObject.lilaClass.getAllSubclasses();
		LookupDAGBuilder builder = new LookupDAGBuilder(classes);
		LookupDAGNode node = builder.buildLookupDAG(gf);
		builder.dump(node);

		ExpressionEnvironment env = new ExpressionEnvironment();
		env.put("lst1", new LilaObject(cons));
		env.put("lst2", new LilaObject(cons));

		System.out.println(node.evaluate(env));
	}

	static void test3() throws Throwable {
		// isomorphic

		final LilaClass treeNode = new LilaClass(false, "TreeNode", null);
		final LilaClass dataNode = new LilaClass(false, "DataNode", null, treeNode);
		final LilaClass emptyNode = new LilaClass(false, "EmptyNode", null, treeNode);

		Set<LilaClass> classes = new HashSet<LilaClass>() {{
			add(treeNode);
			add(dataNode);
			add(emptyNode);
		}};

		final Expression exp1 = new Var("t1");
		exp1.name = "e1";
		exp1.staticClasses = classes;

		final Expression exp2 = new Var("t2");
		exp2.name = "e2";
		exp2.staticClasses = classes;


		final Method m1 = new Method(null);
		m1.identifier = "true";

		final Method m2 = new Method(null);
		m2.identifier = "false";

		final Method m3 = new Method(null);
		m3.identifier = "other";

		Predicate pred1 =
			new AndPredicate(new TypePredicate(exp1, emptyNode),
			                 new TypePredicate(exp2, emptyNode));

		Predicate pred2 =
			new OrPredicate(new TypePredicate(exp1, emptyNode),
			                new TypePredicate(exp2, emptyNode));

		LilaPredicateMethod pm = new LilaPredicateMethod(null, 2, exp1, exp2);

		pm.addMethod(pred1, m1);
		pm.addMethod(pred2, m2);
		pm.addMethod(TruePredicate.INSTANCE, m3);
		pm.dumpMethods();

		LookupDAGBuilder builder = new LookupDAGBuilder(classes);
		LookupDAGNode node = builder.buildLookupDAG(pm);
		builder.dump(node);

		// test evaluation

		ExpressionEnvironment env = new ExpressionEnvironment();
		env.put("t1", new LilaObject(emptyNode));
		env.put("t2", new LilaObject(emptyNode));
		System.err.println(node.evaluate(env) == m1);

		env.put("t1", new LilaObject(emptyNode));
		env.put("t2", new LilaObject(dataNode));
		System.err.println(node.evaluate(env) == m2);

		env.put("t1", new LilaObject(dataNode));
		env.put("t2", new LilaObject(emptyNode));
		System.err.println(node.evaluate(env) == m2);

		env.put("t1", new LilaObject(dataNode));
		env.put("t2", new LilaObject(dataNode));
		System.err.println(node.evaluate(env) == m3);



		// provide information about "compiled" expressions
		pm.setExpressionInfo(exp1, new ExpressionInfo(Test.class.getName(), "test3_exp1", null));
		pm.setExpressionInfo(exp2, new ExpressionInfo(Test.class.getName(), "test3_exp2", null));


		MethodHandle compiled = LookupDAGBuilder.compile(node, 2);
		// TODO: change to MethodHandle
		Method method = (Method)compiled
			.invokeExact(pm,
			             new LilaObject(emptyNode),
			             new LilaObject(emptyNode));
		System.err.println(method == m1);

		Method method2 = (Method)compiled
			.invokeExact(pm,
			             new LilaObject(emptyNode),
			             new LilaObject(dataNode));
		System.err.println(method2 == m2);

		Method method3 = (Method)compiled
			.invokeExact(pm,
			             new LilaObject(dataNode),
			             new LilaObject(emptyNode));
		System.err.println(method3 == m2);

		Method method4 = (Method)compiled
			.invokeExact(pm,
			             new LilaObject(dataNode),
			             new LilaObject(dataNode));
		System.err.println(method4 == m3);
	}

	public static LilaObject test3_exp1 (LilaObject t1, LilaObject t2) {
		System.err.println("In predicate expression exp1");
		return t1;
	}

	public static LilaObject test3_exp2 (LilaObject t1, LilaObject t2) {
		System.err.println("In predicate expression exp2");
		return t2;
	}

}
