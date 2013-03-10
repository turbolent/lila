package lila.runtime.dispatch;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;

import lila.runtime.Expression;
import lila.runtime.ExpressionEnvironment;
import lila.runtime.LilaClass;
import lila.runtime.LilaObject;


abstract class Node {

	abstract Method evaluate(ExpressionEnvironment env);

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


class LilaClassComparator implements Comparator<LilaClass> {

	static LilaClassComparator COMPARATOR = new LilaClassComparator();

	public int compare(LilaClass class1, LilaClass class2) {
		return class1.isSubtypeOf(class2) ? -1 : 1;
	}
}


class InteriorNode extends Node {

	TreeMap<LilaClass,Node> edges =
		new TreeMap<>(LilaClassComparator.COMPARATOR);

	Expression expression;

	@Override
	public Method evaluate(ExpressionEnvironment env) {
		LilaObject v = this.expression.evaluate(env);
		Node target = null;
		for (Entry<LilaClass,Node> entry : this.edges.entrySet())
			if (entry.getKey() == v.getType()) {
				target = entry.getValue();
				break;
			}
		if (target == null) {
			String message =
				String.format("Node evaluation exception: unable "
					+ "to find edge for result %s", v);
			throw new RuntimeException(message);
		}
		return target.evaluate(env);
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
		Map<Node,Set<LilaClass>> edges = new HashMap<>();
		for (Entry<LilaClass, Node> entry : this.edges.entrySet()) {
			Node node = entry.getValue();
			Set<LilaClass> classes = edges.get(node);
			if (classes == null) {
				classes = new LinkedHashSet<>();
				edges.put(node, classes);
			}
			classes.add(entry.getKey());
		}
		for (Entry<Node, Set<LilaClass>> entry : edges.entrySet()) {
			Node targetNode = entry.getKey();
			targetNode.dump(out);
			out.write(String.format("%s -> %s [label=\"%s\"];\n", name,
									targetNode.name, entry.getValue()));
		}
	}
}

class LeafNode extends Node {
	Method method;

	@Override
	public Method evaluate(ExpressionEnvironment env) {
		return this.method;
	}


	// Debugging

	void dump(Writer out) throws IOException {
		out.write(String
			.format("%s [shape=box,label=\"%s\",xlabel=\"cs=%s\\nes=%s\"];\n",
					name, this.method, caseNames(), expressionNames()));
	}
}
