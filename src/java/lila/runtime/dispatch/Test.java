package lila.runtime.dispatch;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import lila.runtime.Expression;
import lila.runtime.ExpressionEnvironment;
import lila.runtime.LilaClass;
import lila.runtime.LilaFalse;
import lila.runtime.LilaGenericFunction;
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

	public static void main(String[] args) throws Exception {

		RT.initialize();

		test1();
		// System.out.println();
		// test2();

		// test3();

	}

	static void test1() throws Exception {
		final LilaClass clazzA = new LilaClass(false, "A", null);
		final LilaClass clazzB = new LilaClass(false, "B", null, clazzA);
		final LilaClass clazzC = new LilaClass(false, "C", null);
		final LilaClass clazzD = new LilaClass(false, "D", null, clazzA, clazzC);

		Expression exp1 = new Var("f1");
		exp1.name = "e1";
		exp1.staticClasses = new HashSet<LilaClass>() {{
			add(clazzA);
			add(clazzB);
			add(clazzC);
		}};

		Expression exp2 = new Var("f1.x");
		exp2.name = "e3";
		exp2.staticClasses = new HashSet<LilaClass>() {{
			add(clazzA);
			add(clazzB);
			add(clazzC);
			add(clazzD);
		}};

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
		exp5.staticClasses = new HashSet<LilaClass>() {{
			add(clazzA);
			add(clazzB);
			add(clazzC);
			add(clazzD);
		}};

		LilaGenericFunction gf = new LilaGenericFunction();

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


		Set<LilaClass> classes = LilaObject.lilaClass.getAllSubclasses();
		DAGBuilder builder = new DAGBuilder(classes);
		Node node = builder.buildLookupDAG(gf);
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

		LilaGenericFunction gf = new LilaGenericFunction();

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
		DAGBuilder builder = new DAGBuilder(classes);
		Node node = builder.buildLookupDAG(gf);
		builder.dump(node);

		ExpressionEnvironment env = new ExpressionEnvironment();
		env.put("lst1", new LilaObject(cons));
		env.put("lst2", new LilaObject(cons));

		System.out.println(node.evaluate(env));
	}

	static void test3() throws Exception {
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

		LilaGenericFunction gf = new LilaGenericFunction();

//		gf.inputExpressions = new ArrayList<Expression>() {{
//			add(exp1);
//			add(exp2);
//		}};

		gf.addMethod(pred1, m1);
		gf.addMethod(pred2, m2);
		gf.addMethod(TruePredicate.INSTANCE, m3);
		gf.dumpMethods();

		DAGBuilder builder = new DAGBuilder(classes);
		Node node = builder.buildLookupDAG(gf);
		builder.dump(node);
	}

}
