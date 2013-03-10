package lila.runtime.dispatch;

import lila.runtime.Expression;
import lila.runtime.LilaTrue;

public class TestPredicate extends Predicate {
	Expression expression;

	public TestPredicate(Expression expression) {
		this.expression = expression;
	}

	@Override
	Predicate prepareForDNF(PredicateEnvironment env) {
		return new TypePredicate(this.expression.resolveBindings(env),
		                         LilaTrue.lilaClass);
	}

	@Override
	public String toString() {
		return String.format("(test %s)", this.expression);
	}
}

