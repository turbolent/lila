package lila.runtime.dispatch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lila.runtime.LilaClass;
import lila.runtime.LilaObject;
import lila.runtime.RT;

public class Test {

	public static void main(String[] args) throws Exception {

		RT.initialize();

		// test1();
		// System.out.println();
		test2();

	}

//	static void test1() throws Exception {
//		final Clazz clazzA = new Clazz("A");
//		final Clazz clazzB = new Clazz("B", clazzA);
//		final Clazz clazzC = new Clazz("C");
//		final Clazz clazzD = new Clazz("D", clazzA, clazzC);
//
//		Expression exp1 = new Var("f1");
//		exp1.name = "e1";
//		exp1.staticClasses = new HashSet<Clazz>() {{
//			add(clazzA);
//			add(clazzB);
//			add(clazzC);
//		}};
//
//		Expression exp2 = new Var("f1.x");
//		exp2.name = "e3";
//		exp2.staticClasses = new HashSet<Clazz>() {{
//			add(clazzA);
//			add(clazzB);
//			add(clazzC);
//			add(clazzD);
//		}};
//
//		Expression exp3 = new Var("f2.x");
//		exp3.name = "e5";
//		exp3.staticClasses = new HashSet<Clazz>() {{
//			add(clazzC);
//			add(clazzD);
//		}};
//
//		Expression exp4 = new BinaryExpression("=", new Var("f1.y"), new Var("f2.y"));
//		exp4.name = "e4";
//		exp4.staticClasses = new HashSet<Clazz>() {{
//			add(TrueClazz.CLAZZ);
//			add(FalseClazz.CLAZZ);
//		}};
//
//		Expression exp5 = new Var("f2");
//		exp5.name = "e2";
//		exp5.staticClasses = new HashSet<Clazz>() {{
//			add(clazzA);
//			add(clazzB);
//			add(clazzC);
//			add(clazzD);
//		}};
//
//
//		// Test GF => DF
//
//		GenericFunction gf = new GenericFunction();
//
//		Predicate pred1 =
//			AndPredicate.fromPredicates(new InstanceofPredicate(exp1, clazzA),
//			                            new BindingPredicate("t", exp2),
//			                            new InstanceofPredicate(new Var("t"), clazzA),
//			                            new NotPredicate(new InstanceofPredicate(new Var("t"), clazzB)),
//			                            new InstanceofPredicate(exp3, clazzC),
//			                            new TestPredicate(exp4));
//		gf.methods.put(pred1, new Method(1));
//
//		Predicate pred2 =
//			new AndPredicate(new InstanceofPredicate(exp2, clazzB),
//			                 new OrPredicate(new AndPredicate(new InstanceofPredicate(exp1, clazzB),
//			                                                  new InstanceofPredicate(exp3, clazzC)),
//			                                 new AndPredicate(new InstanceofPredicate(exp1, clazzC),
//			                                                  new InstanceofPredicate(exp5, clazzA))));
//		gf.methods.put(pred2, new Method(2));
//
//		Predicate pred3 =
//			new AndPredicate(new InstanceofPredicate(exp1, clazzC),
//			                 new InstanceofPredicate(exp5, clazzC));
//		gf.methods.put(pred3, new Method(3));
//
//		Predicate pred4 =
//			new InstanceofPredicate(exp1, clazzC);
//		gf.methods.put(pred4, new Method(4));
//
//		System.out.println(gf);
//
//		DispatchFunction df1 = DispatchFunction.convert(gf);
//
//		System.out.println(df1);
//
//
//		// Test DF => DAG
//		// (DF manually constructed, as example has
//		//  eliminated exp3 / e5, not performed by convert)
//
//		DispatchFunction df2 = new DispatchFunction(exp1, exp5);
//
//		// (f1@A and f1.x@A and f1.x@!B and (f1.y=f2.y)@true) => m1
//		Predicate p1 = AndPredicate.fromPredicates(new InstanceofPredicate(exp1, clazzA),
//		                                           new InstanceofPredicate(exp2, clazzA),
//		                                           new InstanceofPredicate(exp2, new NegatedClazz(clazzB)),
//		                                           new InstanceofPredicate(exp4, TrueClazz.CLAZZ));
//		df2.cases.put(p1, new HashSet<Method>() {{ add(new Method(1)); }});
//
//		final Method m2 = new Method(2);
//
//		// (f1.x@B and f1@B) => m2
//		Predicate p2 = AndPredicate.fromPredicates(new InstanceofPredicate(exp2, clazzB),
//		                                           new InstanceofPredicate(exp1, clazzB));
//		df2.cases.put(p2, new HashSet<Method>() {{ add(m2); }});
//
//		// (f1.x@B and f1@C and f2@A) => m2
//		Predicate p3 = AndPredicate.fromPredicates(new InstanceofPredicate(exp2, clazzB),
//		                                           new InstanceofPredicate(exp1, clazzC),
//		                                           new InstanceofPredicate(exp5, clazzA));
//		df2.cases.put(p3, new HashSet<Method>() {{ add(m2); }});
//
//		// (f1@C and f2@C) => m3
//		Predicate p4 = AndPredicate.fromPredicates(new InstanceofPredicate(exp1, clazzC),
//		                                           new InstanceofPredicate(exp5, clazzC));
//		df2.cases.put(p4, new HashSet<Method>() {{ add(new Method(3)); }});
//
//		// (f1@C) => m4
//		Predicate p5 = new InstanceofPredicate(exp1, clazzC);
//		df2.cases.put(p5, new HashSet<Method>() {{ add(new Method(4)); }});
//
//		System.out.println(df2);
//
//		DAGBuilder builder = new DAGBuilder();
//		Node node = builder.buildLookupDAG(df2);
//		System.out.println(node);
//		builder.dump(node);
//	}

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
		exp1.staticClasses = staticClasses;
//		new HashSet<LilaClass>() {{
//			add(cons);
//		}};

		Expression exp2 = new Var("lst2");
		exp2.name = "e2";
		exp2.staticClasses = staticClasses;

		final Method m1 = new Method(null);
		m1.identifier = "Cons";
		final Method m2 = new Method(null);
		m2.identifier = "Nil";

		Predicate pred1 =
			new AndPredicate(new InstanceofPredicate(exp1, cons),
			                 new InstanceofPredicate(exp2, cons));
		Predicate pred2 =
			new AndPredicate(new InstanceofPredicate(exp1, nil),
			                 new InstanceofPredicate(exp2, nil));


		Map<Predicate, Method> methods = new HashMap<>();
		methods.put(pred1, m1);
		methods.put(pred2, m2);

		Utils.dumpMethods(methods);
		DispatchFunction df = DispatchFunction.fromMethods(methods);
		System.out.println(df);

		DAGBuilder builder = new DAGBuilder();
		Node node = builder.buildLookupDAG(df);
		builder.dump(node);

		ExpressionEnvironment env = new ExpressionEnvironment();
		env.put("lst1", new LilaObject(cons));
		env.put("lst2", new LilaObject(cons));

		System.out.println(node.evaluate(env));
	}
}
