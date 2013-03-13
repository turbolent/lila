package lila.runtime.dispatch;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import lila.runtime.Expression;
import lila.runtime.LilaClass;
import lila.runtime.LilaGenericFunction;
import lila.runtime.LilaObject;



// TODO: DNF simplification?!
// (eliminations, multiple occurrences, implications of rest of clause)


class DAGBuilder {

	// TODO:
	static Method notUnderstood = new Method(null);
	static Method ambiguous = new Method(null);
	static {
		notUnderstood.identifier = "NOT_UNDERSTOOD";
		ambiguous.identifier = "AMBIGUOUS";
	}

	// TODO: check actual dependencies based on static information,
	//       e.g. evaluation of e2 would not encounter an error
	//       even if the test of e1 is false (e.g., simple references
	//       to formals are known statically to be evaluable in any order).
	//       For example, also input parameters are independent!

	static List<List<Expression>> constraints(List<Expression> inputExpressions,
	                                          List<List<Expression>> orderedExpressions) {
		List<List<Expression>> result = new ArrayList<>();
		for (List<Expression> expressions : orderedExpressions) {
			insertion:
			for (int i = 0, l = expressions.size(); i < l; i++) {
				Expression expression = expressions.get(i);
				int targetIndex = -1;
				int partCount = result.size();
				for (int partIndex = 0; partIndex < partCount; partIndex++) {
					List<Expression> part = result.get(partIndex);
					// check if already inserted
					if (part.contains(expression))
						continue insertion;
					// contains no dependencies? (possible) valid target
					boolean noDependenciesInPart = true;
					// input expressions have no dependencies as they
					// are already evaluated
					if (!inputExpressions.contains(expression)) {
						for (int j = 0; j < i; j++) {
							Expression dependency = expressions.get(j);
							if (part.contains(dependency)) {
								noDependenciesInPart = false;
								break;
							}
						}
					}
					if (noDependenciesInPart) {
						targetIndex = partIndex;
						break;
					}
				}
				List<Expression> target;
				if (targetIndex < 0) {
					target = new ArrayList<>();
					result.add(target);
				} else {
					target = result.get(targetIndex);
				}
				// detect and ignore cycle: remaining parts may contain
				// expression (was dependent on expression in target)
				for (int partIndex = targetIndex + 1; partIndex < partCount; partIndex++) {
					List<Expression> part = result.get(partIndex);
					if (part.contains(expression)) {
						part.remove(expression);
						if (part.isEmpty())
							result.remove(part);
						break;
					}
				}
				// finally, add expression to target
				target.add(expression);
			}
		}
		return result;
	}

	List<List<Expression>> constraints;

	List<List<Expression>> constraints(Set<Case> cases, List<Expression> inputExpressions) {
		List<List<Expression>> result = new ArrayList<>();
		for (Case c : cases) {
			List<Expression> expressions = new ArrayList<>();
			expressions.addAll(c.getExpressions());
			result.add(expressions);
		}
		return constraints(inputExpressions, result);
	}

	static Set<Expression> allExpressions(Collection<Case> cases) {
		Set<Expression> result = new LinkedHashSet<>();
		for (Case c : cases)
			result.addAll(c.getExpressions());
		return result;
	}

	Map<Pair<Set<Case>,Set<Expression>>,Node> memo = new HashMap<>();

	Node buildLookupDAG(LilaGenericFunction gf) {
		Set<Case> cases = new HashSet<>();
		cases.addAll(gf.cases.values());
		this.constraints = constraints(cases, gf.inputExpressions);
		return buildSubDAG(cases, allExpressions(cases));
	}

	Node buildSubDAG(Set<Case> cases, Set<Expression> expressions) {
		Pair<Set<Case>,Set<Expression>> pair = new Pair<>(cases, expressions);
		Node node = this.memo.get(pair);
		if (node != null)
			return node;

		if (expressions.isEmpty()) {
			LeafNode leafNode = new LeafNode();
			node = leafNode;
			leafNode.method = computeTarget(cases);
		} else {
			InteriorNode interiorNode = new InteriorNode();
			node = interiorNode;
			Expression expression = pickExpression(expressions, cases);
			interiorNode.expression = expression;
			for (LilaClass clazz : expression.getStaticClasses()) {
				Set<Case> targetCases = targetCases(cases, expression, clazz);
				Set<Expression> targetExpressions =
					targetExpressions(expressions, targetCases, expression);
				Node targetNode = buildSubDAG(targetCases, targetExpressions);
				interiorNode.edges.put(clazz, targetNode);
			}
		}
		this.memo.put(pair, node);

		// Debugging
		node.cases = cases;
		node.expressions = expressions;

		return node;
	}

	Method computeTarget(Set<Case> cases) {
		List<String> caseNames = new ArrayList<>();
		for (Case c : cases)
			caseNames.add(c.name);

		// build minimum set of methods
		Map<Predicate,Set<Method>> methods = new HashMap<>();
		// check implications
		for (Case c : cases) {
			// check if the case's conjunction implies
			// or is implied by an existing conjunction
			boolean isImplied = false;
			for (Entry<Predicate,Set<Method>> entry : methods.entrySet()) {
				Predicate conjunction = entry.getKey();
				if (c.conjunction.implies(conjunction))
					methods.remove(conjunction);
				if (conjunction.implies(c.conjunction)) {
					isImplied = true;
					break;
				}
			}
			if (!isImplied)
				methods.put(c.conjunction, c.methods);
		}
		// gather all methods
		Set<Method> allMethods = new HashSet<>();
		for (Entry<Predicate,Set<Method>> entry : methods.entrySet())
			allMethods.addAll(entry.getValue());
		switch (allMethods.size()) {
		case 0:
			return notUnderstood;
		case 1:
			return allMethods.iterator().next();
		default:
			return ambiguous;
		}
	}

	Set<Case> targetCases(Set<Case> cases, Expression expression, LilaClass clazz) {
		Set<Case> result = new HashSet<>();
		for (Case c : cases) {
			Set<LilaClass> passingClasses = classesPassingTest(c, expression);
			if (passingClasses.contains(clazz))
				result.add(c);
		}
		return result;
	}

	Set<LilaClass> classesPassingTest(Case c, Expression expression) {
		Set<LilaClass> result = null;
		if (c.getExpressions().contains(expression)) {
			for (Predicate predicate : c.getAtoms()) {
				TypePredicate atom = (TypePredicate)predicate;
				if (atom.expression.equals(expression)) {
					Set<LilaClass> classes = atom.getClasses();
					// intersection
					if (result == null) {
						result = new HashSet<>();
						result.addAll(classes);
					} else {
						result.retainAll(classes);
					}
				}
			}
		} else {
			// all classes
			result = LilaObject.lilaClass.getAllSubclasses();
		}
		return result;

	}

	Set<Expression> targetExpressions
		(Set<Expression> expressions, Set<Case> targetCases, Expression expression)
	{
		Set<Expression> result = new HashSet<>();
		result.addAll(expressions);
		result.remove(expression);
		Set<Expression> allExpressions = new HashSet<>();
		for (Case c : targetCases)
			allExpressions.addAll(c.getExpressions());
		result.retainAll(allExpressions);
		return result;
	}

	Expression pickExpression(Set<Expression> expressions, Set<Case> cases) {
		Set<Expression> legalExpressions = minExpressions(expressions);
		// TODO: heuristic: avgNumTargetCase
		Expression expression = Collections.min(legalExpressions, new Comparator<Expression>() {
			public int compare(Expression o1, Expression o2) {
				return o1.getCost().compareTo(o2.getCost());
			}
		});
		return expression;
	}

	Set<Expression> minExpressions(Set<Expression> expressions) {
		Set<Expression> result = new HashSet<>();
		// use constraints to determine safe to evaluate expressions
		for (Expression expression : expressions) {
			// find part
			int pos = -1;
			for (int i = 0, l = this.constraints.size(); i < l; i++) {
				List<Expression> part = this.constraints.get(i);
				if (part.contains(expression))
					pos = i;
			}
			// check for dependencies
			boolean hasDependencies = false;
			search:
			for (int i = 0; i < pos; i++) {
				List<Expression> part = this.constraints.get(i);
				if (Utils.containsAny(part, expressions)) {
					hasDependencies = true;
					break search;
				};
			}
			if (!hasDependencies)
				result.add(expression);
		}
		return result;
	}

	// Debugging

	void dump(Node node) throws Exception {
		FileWriter fileWriter = new FileWriter("lookupDAG.dot");
		BufferedWriter writer = new BufferedWriter(fileWriter);
		writer.write("digraph lookupDAG {\n");
		writer.write("rankdir=LR; ranksep=1; nodesep=0.7; forcelabels=true;\n");
		writer.write("node [fontsize=10]; edge [fontsize=10];\n");
		node.dump(writer);
		writer.write("\n}");
		writer.close();
		fileWriter.close();
	}
}


