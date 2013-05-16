package lila.runtime.dispatch.predicate;

import java.util.LinkedHashSet;
import java.util.Set;

import lila.runtime.Expression;

public class Case {
	public Predicate conjunction;
	public Set<Method> methods = new LinkedHashSet<>();

	public Case(Predicate conjunction) {
		this.conjunction = conjunction;
	}

	// NOTE: constraint calculation depends on order:
	//       LinkedHashMap keeps order
	LinkedHashSet<Expression> getExpressions() {
		LinkedHashSet<Expression> result = new LinkedHashSet<>();
		for (Predicate atom : this.conjunction.getAtoms())
			result.add(((TypePredicate)atom).expression);
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
