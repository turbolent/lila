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
			result.add(((InstanceofPredicate)atom).expression);
		return result;
	}

	Set<Predicate> getAtoms() {
		return conjunction.getAtoms();
	}

	@Override
	public String toString() {
		return String.format("%s%s", this.conjunction, this.methods);
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

	Set<Case> cases = new HashSet<>();

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		String sep = "  ";
		for (Case c : this.cases) {
			builder.append(String.format("\n  %s %s => %s}", sep,
			                             c.conjunction, c.methods));
			sep = "or";
		}
		return "DF" + builder;
	}

	//// Transformation: GF => DF

	static DispatchFunction fromMethods(Map<Predicate, Method> methods)  {
		DispatchFunction result = new DispatchFunction();
		// build map
		HashMap<Predicate, Set<Method>> caseMap = new LinkedHashMap<>();
		for (Entry<Predicate, Method> entry : methods.entrySet()) {
			Predicate predicate = entry.getKey();
			final Method method = entry.getValue();
			predicate = predicate.canonicalize();
			addDisjunction(caseMap, predicate, method);
		}
		// convert map to set of cases
		for (Entry<Predicate, Set<Method>> entry : caseMap.entrySet())
			result.cases.add(new Case(entry.getKey(), entry.getValue()));
		return result;
	}

	static void addDisjunction(HashMap<Predicate, Set<Method>> caseMap,
		Predicate disjunction, Method method)
	{
		// Step 5: split disjunctions, add method for each conjunction
		if (disjunction instanceof OrPredicate) {
			OrPredicate predicate = (OrPredicate)disjunction;
			addDisjunction(caseMap, predicate.left, method);
			addDisjunction(caseMap, predicate.right, method);
		} else
			addConjunction(caseMap, disjunction, method);
	}

	static void addConjunction(HashMap<Predicate, Set<Method>> caseMap,
		     	 			   Predicate conjunction, Method method)
	{
		if (conjunction instanceof AndPredicate)
			conjunction = ((AndPredicate)conjunction).flatten();

		// Step 7: remove all tests guaranteed to be true
		conjunction = conjunction.removeTrueAtoms();
		if (conjunction == null)
			return;

		// Step 8: eliminate conjunctions containing
		// atomic tests  that are guaranteed to be false
		conjunction = conjunction.removeFalseConjunctions();
		if (conjunction == null)
			return;

		// Step 9: merging of duplicate conjunctions
		// (implicit through hash map)
		Set<Method> methods = caseMap.get(conjunction);
		if (methods == null) {
			methods = new HashSet<Method>();
			caseMap.put(conjunction, methods);
		}
		methods.add(method);
	}


}
