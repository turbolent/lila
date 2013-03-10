package lila.runtime.dispatch;

import lila.runtime.Expression;

public class BindingPredicate extends Predicate {
	String name;
	Expression expression;

	public BindingPredicate(String name, Expression expression) {
		this.name = name;
		this.expression = expression;
	}

	@Override
	public Predicate prepareForDNF(PredicateEnvironment env) {
		env.put(this.name, this.expression.resolveBindings(env));
		return null;
	}

	@Override
	public String toString() {
		return String.format("(%s := %s)", this.name, this.expression);
	}
}
