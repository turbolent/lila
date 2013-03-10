package lila.runtime.dispatch;

public class AndPredicate extends BinaryPredicate {

	public AndPredicate(Predicate left, Predicate right) {
		super(left, right);
	}

	@Override
	boolean implies(Predicate predicate) {
		if (super.implies(predicate))
			return true;
		// TODO: check
		return this.left.implies(predicate)
			|| this.right.implies(predicate);
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

	@Override
	public Predicate removeFalseConjunctions() {
		Predicate result = super.removeFalseConjunctions();
		if (result == null || !(result instanceof AndPredicate))
			return result;
		AndPredicate conjunction = (AndPredicate)result;
		if (conjunction.left.isAlwaysFalse()
			|| conjunction.right.isAlwaysFalse())
		{
			return null;
		} else
			return result;
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

