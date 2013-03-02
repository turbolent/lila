package lila.runtime.dispatch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;


class GenericFunction {
	Map<Predicate, Method> methods = new HashMap<>();

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Entry<Predicate, Method> entry : this.methods.entrySet()) {
			builder.append(String.format(	"\n  when %s %s", entry.getKey(),
											entry.getValue()));
		}
		return "GF" + builder;
	}
}

class Method {
	// TODO: MethodHandle?
	Object body;

	Method(Object body) {
		this.body = body;
	}

	@Override
	public String toString() {
		return String.format("{ %s }", this.body.toString());
	}
}

class Case {
	Predicate conjunction;
	Set<Method> methods;

	public Case(Predicate conjunction, Set<Method> methods) {
		this.conjunction = conjunction;
		this.methods = methods;
	}

	// NOTE: constraint calculation depends on order:
	//       LinkedHashMap keeps order
	LinkedHashSet<Expression> getExpressions() {
		LinkedHashSet<Expression> result = new LinkedHashSet<>();
		for (Predicate atom : this.conjunction.getAtoms())
			result.add(((InstanceofPredicate) atom).expression);
		return result;
	}

	Set<Predicate> getAtoms() {
		return conjunction.getAtoms();
	}

	@Override
	public String toString() {
		return String
			.format("#[Case %s => %s]", this.conjunction, this.methods);
	}

	// Debugging

	String name = "c" + ids++;
	static int ids = 1;
}

class DispatchFunction {

	List<Expression> inputExpressions;

	public DispatchFunction(Expression... inputExpressions) {
		// TODO: remove, debugging
		int cost = 0;
		for (Expression inputExpression : inputExpressions)
			inputExpression.cost = cost++;

		this.inputExpressions = Arrays.asList(inputExpressions);
	}

	Map<Predicate, Set<Method>> cases = new LinkedHashMap<>();

	Set<Case> getCases() {
		Set<Case> result = new HashSet<>();
		for (Entry<Predicate, Set<Method>> entry : this.cases.entrySet())
			result.add(new Case(entry.getKey(), entry.getValue()));
		return result;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		String sep = "  ";
		for (Entry<Predicate, Set<Method>> entry : this.cases.entrySet()) {
			builder.append(String.format("\n  %s %s => %s}", sep,
			                             entry.getKey(), entry.getValue()));
			sep = "or";
		}
		return "DF" + builder;
	}

	void addConjunction(Predicate conjunction, Method method) {
		if (conjunction instanceof AndPredicate)
			conjunction = ((AndPredicate) conjunction).flatten();

		// TODO: step 7, 8: use static class information to eliminate
		// atomic tests that are guaranteed to be true and
		// all conjunctions containing atomic tests that are
		// guaranteed to be false

		// Step 9: merging of duplicate conjunctions (implicit)
		Set<Method> methods = this.cases.get(conjunction);
		if (methods == null) {
			methods = new HashSet<Method>();
			this.cases.put(conjunction, methods);
		}
		methods.add(method);
	}

	void addDisjunction(Predicate disjunction, Method method) {
		// Step 5: split disjunctions, add method for each conjunction
		if (disjunction instanceof OrPredicate) {
			OrPredicate predicate = (OrPredicate) disjunction;
			this.addDisjunction(predicate.left, method);
			this.addDisjunction(predicate.right, method);
		} else
			this.addConjunction(disjunction, method);
	}

	static DispatchFunction convert(GenericFunction gf) {
		DispatchFunction result = new DispatchFunction();
		for (Entry<Predicate, Method> entry : gf.methods.entrySet()) {
			Predicate predicate = entry.getKey();
			final Method method = entry.getValue();
			predicate = predicate.canonicalize();
			result.addDisjunction(predicate, method);
		}
		return result;
	}
}
