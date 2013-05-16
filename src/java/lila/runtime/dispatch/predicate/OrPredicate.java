package lila.runtime.dispatch.predicate;

public class OrPredicate extends BinaryPredicate {

	public OrPredicate(Predicate left, Predicate right) {
		super(left, right);
	}


	@Override
	public String toString() {
		return String.format("(%s or %s)", this.left, this.right);
	}
}
