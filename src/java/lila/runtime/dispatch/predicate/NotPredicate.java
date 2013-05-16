package lila.runtime.dispatch.predicate;

import lila.runtime.Evaluator;
import lila.runtime.Compiler;

public class NotPredicate extends Predicate {
	Predicate predicate;

	public NotPredicate(Predicate predicate) {
		this.predicate = predicate;
	}

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

	// TODO: not in DF, remove?
	@Override
	Predicate postProcessDNF() {
		if (this.predicate instanceof TypePredicate) {
			TypePredicate predicate =
				(TypePredicate) this.predicate;
			return new TypePredicate(predicate.expression,
			                         predicate.type.negate());
		} else
			return this;
	}

	@Override
	void resolveTypes(Evaluator evaluator) {
		this.predicate.resolveTypes(evaluator);
	}

	@Override
	public void compileExpressions(Compiler compiler) {
		this.predicate.compileExpressions(compiler);
	}

	@Override
	public String toString() {
		return String.format("(not %s)", this.predicate);
	}
}
