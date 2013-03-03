package lila.runtime.dispatch;

import java.util.HashSet;
import java.util.Set;

import lila.runtime.LilaClass;
import lila.runtime.LilaObject;

abstract class Expression {
	// TODO: LilaBoolean ?
	abstract LilaObject evaluate();

	abstract Expression resolve(Environment env);

	Expression negate() {
		return new NotExpression(this);
	}

	// Static information

	Set<LilaClass> staticClasses;

	Set<LilaClass> getStaticClasses() {
		return (this.staticClasses == null ? new HashSet<LilaClass>()
			: this.staticClasses);
	}

	long cost = -1;

	public Long getCost() {
		return (this.cost == -1 ? Long.MAX_VALUE : this.cost);
	}

	// Debugging

	String name;
}

class NotExpression extends Expression {
	Expression expression;

	NotExpression(Expression expression) {
		this.expression = expression;
	}

	@Override
	public String toString() {
		return "not(" + this.expression + ")";
	}

	@Override
	LilaObject evaluate() {
		// TODO
		return null;
	}

	Expression resolve(Environment env) {
		this.expression = this.expression.resolve(env);
		return this;
	}

}

class Var extends Expression {
	String identifier;

	Var(String identifier) {
		this.identifier = identifier;
	}

	@Override
	public LilaObject evaluate() {
		// TODO
		return null;
	}

	@Override
	public String toString() {
		return this.identifier;
	}

	Expression resolve(Environment env) {
		Expression subst = env.get(this.identifier);
		if (subst == null)
			return this;
		else
			return subst;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
			+ ((identifier == null) ? 0 : identifier.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Var other = (Var) obj;
		if (identifier == null) {
			if (other.identifier != null)
				return false;
		} else if (!identifier.equals(other.identifier))
			return false;
		return true;
	}
}

class BinaryExpression extends Expression {
	String operation;
	Expression left;
	Expression right;

	BinaryExpression(String operation, Expression left, Expression right) {
		this.operation = operation;
		this.left = left;
		this.right = right;
	}

	@Override
	public LilaObject evaluate() {
		// TODO
		return null;
	}

	@Override
	public String toString() {
		return String.format("(%s %s %s)",
		                     this.left, this.operation, this.right);
	}

	@Override
	Expression resolve(Environment env) {
		this.left = this.left.resolve(env);
		this.right = this.right.resolve(env);
		return this;
	}
}
