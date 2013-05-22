package lila.runtime.dispatch.predicate;

public class TruePredicate extends Predicate {

	public static TruePredicate INSTANCE = new TruePredicate();

	private TruePredicate() {};

	@Override
	public String toString() {
		return "true";
	}
}
