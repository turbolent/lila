package lila.runtime.dispatch;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import lila.runtime.LilaClass;
import lila.runtime.LilaObject;
import lila.runtime.LilaNegatedClass;
import lila.runtime.LilaTrue;


class PredicateEnvironment extends HashMap<String, Expression> {}

abstract class Predicate {
	// TODO:
	// abstract boolean evaluate();

	Predicate canonicalize() {
		return this.prepareForDNF(new PredicateEnvironment())
			.toNNF().toDNF().postProcessDNF();
	}

	// Step 1-2: resolve variables, remove empty predicates
	Predicate prepareForDNF(PredicateEnvironment env) {
		return this;
	};

	// Step 3a: push down negations
	Predicate toNNF() {
		return this;
	};

	// Step 3b:
	Predicate toDNF() {
		return this;
	};

	// Step 4:
	Predicate postProcessDNF() {
		return this;
	}

	Set<Predicate> getAtoms() {
		return Collections.emptySet();
	}

	boolean implies(Predicate predicate) {
		return this.equals(predicate);
	}
}

class InstanceofPredicate extends Predicate {
	Expression expression;
	LilaClass type;

	InstanceofPredicate(Expression expression, LilaClass type) {
		this.expression = expression;
		this.type = type;
	}

	// TODO:
	// @Override
	// boolean evaluate() {
	// return this.clazz.isInstance(expression.evaluate());
	// }

	@Override
	boolean implies(Predicate predicate) {
		if (predicate instanceof InstanceofPredicate) {
			InstanceofPredicate p = (InstanceofPredicate) predicate;
			return p.expression.equals(this.expression)
				&& p.type.getAllSubclasses().contains(this.type);
		}
		return false;
	}

	@Override
	public Predicate prepareForDNF(PredicateEnvironment env) {
		this.expression = this.expression.resolve(env);
		return this;
	}

	Set<Predicate> getAtoms() {
		Set<Predicate> atoms = new HashSet<>();
		atoms.add(this);
		return atoms;
	}

	Set<LilaClass> getClasses() {
		Set<LilaClass> result = new HashSet<>();
		if (this.type instanceof LilaNegatedClass) {
			result.addAll(LilaObject.lilaClass.getAllSubclasses());
			result.removeAll(this.type.getAllSubclasses());
		} else {
			result.addAll(this.type.getAllSubclasses());
		}
		return result;
	}

	@Override
	public String toString() {
		return String.format("%s@%s", this.expression, this.type);
	}
}

class TestPredicate extends Predicate {
	Expression expression;

	TestPredicate(Expression expression) {
		this.expression = expression;
	}

	// TODO:
	// @Override
	// public boolean evaluate() {
	// return expression.evaluate().isTrue();
	// }

	// TODO: not in DF, remove?
	@Override
	boolean implies(Predicate predicate) {
		if (predicate instanceof TestPredicate)
			return ((TestPredicate) predicate).expression
				.equals(this.expression);
		return false;
	}

	@Override
	public Predicate prepareForDNF(PredicateEnvironment env) {
		return new InstanceofPredicate(this.expression.resolve(env),
										LilaTrue.lilaClass);
	}

	// not in DNF -> no getAtoms

	@Override
	public String toString() {
		return String.format("(test %s)", this.expression);
	}
}

class BindingPredicate extends Predicate {
	String name;
	Expression expression;

	BindingPredicate(String name, Expression expression) {
		this.name = name;
		this.expression = expression;
	}

	// TODO:
	// @Override
	// public boolean evaluate() {
	// return true;
	// }

	@Override
	public Predicate prepareForDNF(PredicateEnvironment env) {
		env.put(this.name, this.expression.resolve(env));
		return null;
	}

	// not in DNF -> no getAtoms

	@Override
	public String toString() {
		return String.format("(%s := %s)", this.name, this.expression);
	}
}

class NotPredicate extends Predicate {
	Predicate predicate;

	NotPredicate(Predicate predicate) {
		this.predicate = predicate;
	}

	// TODO:
	// @Override
	// public boolean evaluate() {
	// return !predicate.evaluate();
	// }

	@Override
	Predicate prepareForDNF(PredicateEnvironment env) {
		Predicate prepared = this.predicate.prepareForDNF(env);
		if (prepared == null)
			return null;
		else {
			this.predicate = prepared;
			return this;
		}
	}

	@Override
	public Predicate toNNF() {
		Predicate prepared = this.predicate.toNNF();
		if (prepared instanceof NotPredicate) {
			return ((NotPredicate) prepared).predicate;
		} else if (prepared instanceof AndPredicate) {
			AndPredicate predicate = (AndPredicate) prepared;
			return new OrPredicate(new NotPredicate(predicate.left).toNNF(),
									new NotPredicate(predicate.right).toNNF());
		} else if (prepared instanceof OrPredicate) {
			OrPredicate predicate = (OrPredicate) prepared;
			return new AndPredicate(new NotPredicate(predicate.left).toNNF(),
									new NotPredicate(predicate.right).toNNF());
		} else {
			this.predicate = prepared;
			return this;
		}
	}

	@Override
	Predicate postProcessDNF() {
		if (this.predicate instanceof InstanceofPredicate) {
			InstanceofPredicate predicate =
				(InstanceofPredicate) this.predicate;
			return new InstanceofPredicate(predicate.expression,
											predicate.type.negate());
		} else
			return this;
	}

	// not in DNF -> no getAtoms

	@Override
	public String toString() {
		return String.format("(not %s)", this.predicate);
	}
}

abstract class BinaryPredicate extends Predicate {
	Predicate left;
	Predicate right;

	BinaryPredicate(Predicate left, Predicate right) {
		this.left = left;
		this.right = right;
	}

	@Override
	Predicate prepareForDNF(PredicateEnvironment env) {
		Predicate preparedLeft = this.left.prepareForDNF(env);
		Predicate preparedRight = this.right.prepareForDNF(env);
		if (preparedLeft != null && preparedRight != null) {
			this.left = preparedLeft;
			this.right = preparedRight;
			return this;
		} else if (preparedLeft != null)
			return preparedLeft;
		else if (preparedRight != null)
			return preparedRight;
		else
			return null;
	};

	@Override
	BinaryPredicate toNNF() {
		this.left = this.left.toNNF();
		this.right = this.right.toNNF();
		return this;
	}

	@Override
	BinaryPredicate toDNF() {
		this.left = this.left.toDNF();
		this.right = this.right.toDNF();
		return this;
	}

	@Override
	BinaryPredicate postProcessDNF() {
		this.left = this.left.postProcessDNF();
		this.right = this.right.postProcessDNF();
		return this;
	}

	@Override
	Set<Predicate> getAtoms() {
		Set<Predicate> atoms = new LinkedHashSet<>();
		atoms.addAll(this.left.getAtoms());
		atoms.addAll(this.right.getAtoms());
		return atoms;
	}
}

class AndPredicate extends BinaryPredicate {

	AndPredicate(Predicate left, Predicate right) {
		super(left, right);
	}

	// TODO:
	// @Override
	// public boolean evaluate() {
	// return left.evaluate()
	// && right.evaluate();
	// }

	@Override
	boolean implies(Predicate predicate) {
		// TODO: check
		return this.left.implies(predicate) || this.right.implies(predicate);
	}

	@Override
	public String toString() {
		return String.format("(%s and %s)", this.left, this.right);
	}

	@Override
	BinaryPredicate toDNF() {
		BinaryPredicate processed = super.toDNF();
		boolean leftIsOr = processed.left instanceof OrPredicate;
		boolean rightIsOr = processed.right instanceof OrPredicate;
		// NOTE: no reordering to keep evaluation order
		// (p ∨ q) ∧ r → (p ∧ r) ∨ (q ∧ r)
		if (leftIsOr && !rightIsOr) {
			BinaryPredicate left = (BinaryPredicate) processed.left;
			return new OrPredicate(new AndPredicate(left.left, processed.right),
			                       new AndPredicate(left.right, processed.right));
		}
		// p ∧ (q ∨ r) → (p ∧ q) ∨ (p ∧ r)
		else if (!leftIsOr && rightIsOr) {
			BinaryPredicate right = (BinaryPredicate) processed.right;
			return new OrPredicate(new AndPredicate(processed.left, right.left),
			                       new AndPredicate(processed.left, right.right));
		}
		// (p ∨ q) ∧ (r ∨ s) → ((p ∧ r) ∨ (p ∧ s)) ∨ ((q ∧ r) ∨ (q ∧ s))
		else if (leftIsOr && rightIsOr) {
			BinaryPredicate left = (BinaryPredicate) processed.left;
			BinaryPredicate right = (BinaryPredicate) processed.right;
			return new OrPredicate(new OrPredicate(new AndPredicate(left.left,
			                                                        right.left),
			                                       new AndPredicate(left.left,
			                                                        right.right)),
                                   new OrPredicate(new AndPredicate(left.right,
                                                                    right.left),
                                                   new AndPredicate(left.right,
																	right.right)));
		} else
			return processed;
	}

	AndPredicate flatten() {
		if (this.left instanceof AndPredicate) {
			AndPredicate predicate = (AndPredicate) this.left;
			this.left = predicate.left;
			this.right = new AndPredicate(predicate.right, this.right);
			return this.flatten();
		} else {
			if (this.right instanceof AndPredicate)
				this.right = ((AndPredicate) this.right).flatten();
			return this;
		}
	}

	// Convenience

	static Predicate fromPredicates(Predicate... predicates) {
		int l = predicates.length;
		Predicate result = predicates[l - 1];
		for (int i = l - 2; i >= 0; i--) {
			Predicate other = predicates[i];
			result = new AndPredicate(other, result);
		}
		return result;
	}
}

class OrPredicate extends BinaryPredicate {

	OrPredicate(Predicate left, Predicate right) {
		super(left, right);
	}

	// TODO:
	// @Override
	// public boolean evaluate() {
	// return left.evaluate()
	// || right.evaluate();
	// }

	@Override
	public String toString() {
		return String.format("(%s or %s)", this.left, this.right);

	}
}

class TruePredicate extends Predicate {
	// TODO:
	// @Override
	// public boolean evaluate() {
	// return true;
	// }

	@Override
	public String toString() {
		return "true";
	}
}
