package lila.runtime.dispatch;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import lila.runtime.LilaClass;


abstract class Node {
	abstract Method evaluate();


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

class InteriorNode extends Node {
	Map<Node, Set<LilaClass>> edges = new HashMap<>();
	Expression expression;

	@Override
	public Method evaluate() {
		return null;
		// TODO:
		// Instance v = this.expression.evaluate();
		// Node target = null;
		// for (Edge edge : this.edges)
		// if (edge.clazz.isInstance(v)) {
		// target = edge.target;
		// break;
		// }
		// if (target == null) {
		// String message = String.format("Node evaluation exception: unable "
		// + "to find edge for result %s", v);
		// throw new RuntimeException(message);
		// }
		// return target.evaluate();
	}

	void addEdge(LilaClass c, Node targetNode) {
		Set<LilaClass> classes = edges.get(targetNode);
		if (classes == null) {
			classes = new LinkedHashSet<>();
			edges.put(targetNode, classes);
		}
		classes.add(c);
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
		for (Node targetNode : this.edges.keySet())
			targetNode.dump(out);
		for (Entry<Node, Set<LilaClass>> entry : this.edges.entrySet())
			out.write(String.format("%s -> %s [label=\"%s\"];\n", name,
									entry.getKey().name, entry.getValue()));
	}
}

class LeafNode extends Node {
	Method method;

	@Override
	public Method evaluate() {
		return this.method;
	}


	// Debugging

	void dump(Writer out) throws IOException {
		out.write(String
			.format("%s [shape=box,label=\"%s\",xlabel=\"cs=%s\\nes=%s\"];\n",
					name, this.method, caseNames(), expressionNames()));
	}
}
