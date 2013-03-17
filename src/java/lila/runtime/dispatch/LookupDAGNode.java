package lila.runtime.dispatch;

import static java.lang.invoke.MethodType.methodType;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import lila.runtime.Expression;
import lila.runtime.ExpressionEnvironment;
import lila.runtime.ExpressionInfo;
import lila.runtime.LilaClass;
import lila.runtime.LilaGenericFunction;
import lila.runtime.LilaObject;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;



abstract class LookupDAGNode {

	protected LilaGenericFunction gf;

	public LookupDAGNode(LilaGenericFunction gf) {
		this.gf = gf;
	}

	abstract Method evaluate(ExpressionEnvironment env);

	abstract void compileASM(MethodVisitor mv, int argumentCount);


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

	private Expression expression;

//	DispatchTreeNode dispatchTree;

	public void addEdge(LookupDAGEdge edge) {
		edges.add(edge);
	}

	@Override
	public Method evaluate(ExpressionEnvironment env) {
		LilaObject v = this.expression.evaluate(env);
		DispatchTreeNode dispatchTree =
			new DispatchTreeBuilder().buildDispatchTree(this, null);
		LookupDAGNode target =
			dispatchTree.evaluate(v.getType().getIdentifier());
		return target.evaluate(env);
	}

	@Override
	void compileASM(MethodVisitor mv, int argumentCount) {
		DispatchTreeNode dispatchTree =
			new DispatchTreeBuilder().buildDispatchTree(this, null);

		try {
			DispatchTreeBuilder.dump(dispatchTree);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// load arguments
		int arity = this.gf.getArity();
		for (int index = 0; index < arity; index++)
			// +1: function
			mv.visitVarInsn(Opcodes.ALOAD, index + 1);

		// invoke compiled expression method
		Class<?>[] parameterTypes = new Class<?>[arity];
		for (int index = 0; index < arity; index++)
			parameterTypes[index] = LilaObject.class;

		ExpressionInfo expressionInfo =
			this.gf.getExpressionInfo(this.expression);
		String expressionClassName =
			expressionInfo.getClassName().replace('.', '/');
		String expressionMethodName = expressionInfo.getMethodName();
		mv.visitMethodInsn(Opcodes.INVOKESTATIC,
		                   expressionClassName, expressionMethodName,
		                   methodType(LilaObject.class, parameterTypes)
		                   		.toMethodDescriptorString());

		// get type identifier
		String lilaObjectClassName =
			LilaObject.class.getName().replace('.', '/');
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
		                   lilaObjectClassName, "getType",
		                   methodType(LilaClass.class)
		                   		.toMethodDescriptorString());
		String lilaClassClassName =
			LilaClass.class.getName().replace('.', '/');
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
		                   lilaClassClassName, "getIdentifier",
		                   methodType(int.class)
		                   		.toMethodDescriptorString());

		dispatchTree.compileASM(mv, argumentCount);
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
	void compileASM(MethodVisitor mv, int argumentCount) {
		// get method index
		int methodIndex = this.gf.getMethods().indexOf(this.method);
		mv.visitLdcInsn(methodIndex);
		// store in local variable after all arguments (+1: function)
		mv.visitVarInsn(Opcodes.ISTORE, argumentCount + 1);
	}

	// Debugging

	void dump(Writer out) throws IOException {
		out.write(String
			.format("%s [shape=box,label=\"%s\",xlabel=\"cs=%s\\nes=%s\"];\n",
					name, this.method, caseNames(), expressionNames()));
	}
}