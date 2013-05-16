package lila.runtime.dispatch.predicate;

public class TruePredicate extends Predicate {

	static TruePredicate INSTANCE = new TruePredicate();

	private TruePredicate() {};

	@Override
	public String toString() {
		return "true";
	}
}
