package lila.runtime.dispatch;

import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import lila.runtime.Expression;
import lila.runtime.ExpressionEnvironment;
import lila.runtime.ExpressionInfo;
import lila.runtime.LilaClass;
import lila.runtime.LilaGenericFunction;
import lila.runtime.LilaObject;


abstract class LookupDAGNode {

	protected LilaGenericFunction gf;

	public LookupDAGNode(LilaGenericFunction gf) {
		this.gf = gf;
	}

	abstract Method evaluate(ExpressionEnvironment env);

	abstract void compileASM(MethodVisitor mv);


	// Debugging

	String name = "n" + count++;
	static int count = 1;

	abstract void dump(Writer out) throws IOException;

	Set<Case> cases;
	Set<Expression> expressions;

	List<String> expressionNames() {
		List<String> result = new ArrayList<>();
		for (Expression expression : this.expressions)
			result.add(expression.name);
		return result;
	}

	List<String> caseNames() {
		List<String> result = new ArrayList<>();
		for (Case c : this.cases)
			result.add(c.name);
		return result;
	}
}

class LookupDAGEdge {
	LilaClass type;
	LookupDAGNode targetNode;
	LookupDAGEdge(LilaClass type, LookupDAGNode targetNode) {
		this.type = type;
		this.targetNode = targetNode;
	}
}


class LookupDAGInteriorNode extends LookupDAGNode {

	public LookupDAGInteriorNode
		(LilaGenericFunction gf, Expression expression)
	{
		super(gf);
		this.expression = expression;
	}

	List<LookupDAGEdge> edges = new ArrayList<>();

	Map<LilaClass,LookupDAGNode> map = new HashMap<>();

	Expression expression;

//	DispatchTreeNode dispatchTree;

	public void addEdge(LookupDAGEdge edge) {
		edges.add(edge);
		map.put(edge.type, edge.targetNode);
	}

	@Override
	public Method evaluate(ExpressionEnvironment env) {
		LilaObject v = this.expression.evaluate(env);
//		LookupDAGNode target =
//			this.dispatchTree.evaluate(v.getType().identifier);
//		return target.evaluate(env);
		return map.get(v.getType()).evaluate(env);
	}

	@Override
	void compileASM(MethodVisitor mv) {
		DispatchTreeNode dispatchTree =
			DispatchTreeBuilder.buildDispatchTree(this);

		try {
			DispatchTreeBuilder.dump(dispatchTree);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO:
//		MethodHandle expressionMethod =
//			this.gf.getExpression(this.expression);
//		expressionMethod.
		ExpressionInfo info = this.gf.getExpressionInfo(this.expression);
		System.err.println("I N: " + info.getClassName());
		// mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, arg1, arg2, arg3)
	}

	// Debugging

	boolean dumped = false;

	void dump(Writer out) throws IOException {

		if (dumped)
			return;
		dumped = true;

		out.write(String
			.format("%s [label=\"%s\", xlabel=\"cs=%s\\nes=%s\"];\n", name,
					this.expression.name, caseNames(), expressionNames()));
		// group edges by class
		Map<LookupDAGNode,Set<LilaClass>> edges = new HashMap<>();
		for (LookupDAGEdge edge : this.edges) {
			LookupDAGNode node = edge.targetNode;
			Set<LilaClass> classes = edges.get(node);
			if (classes == null) {
				classes = new LinkedHashSet<>();
				edges.put(node, classes);
			}
			classes.add(edge.type);
		}
		for (Entry<LookupDAGNode, Set<LilaClass>> entry : edges.entrySet()) {
			LookupDAGNode targetNode = entry.getKey();
			targetNode.dump(out);
			out.write(String.format("%s -> %s [label=\"%s\"];\n", name,
									targetNode.name, entry.getValue()));
		}
	}
}

class LookupDAGLeafNode extends LookupDAGNode {
	Method method;

	public LookupDAGLeafNode(LilaGenericFunction gf, Method method) {
		super(gf);
		this.method = method;
	}

	@Override
	public Method evaluate(ExpressionEnvironment env) {
		return this.method;
	}

	@Override
	public String toString() {
		return this.method.toString();
	}

	@Override
	void compileASM(MethodVisitor mv) {
		// TODO
	}

	// Debugging

	void dump(Writer out) throws IOException {
		out.write(String
			.format("%s [shape=box,label=\"%s\",xlabel=\"cs=%s\\nes=%s\"];\n",
					name, this.method, caseNames(), expressionNames()));
	}
}
