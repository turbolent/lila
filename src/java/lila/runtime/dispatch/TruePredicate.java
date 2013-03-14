package lila.runtime.dispatch;

public class TruePredicate extends Predicate {

	static TruePredicate INSTANCE = new TruePredicate();

	private TruePredicate() {};

	@Override
	public String toString() {
		return "true";
	}
}
