package lila.runtime.dispatch;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import lila.runtime.Compiler;
import lila.runtime.Evaluator;


public abstract class Predicate {

	public Set<Predicate> canonicalize() {
		Predicate predicate = this.prepareForDNF(new PredicateEnvironment())
			.toNNF().toDNF().postProcessDNF();

		HashSet<Predicate> conjunctions = new LinkedHashSet<>();
		Predicate.addDisjunction(conjunctions, predicate);
		return conjunctions;
	}

	private static void addDisjunction(Set<Predicate> conjunctions, Predicate disjunction) {
		// Step 5: split disjunctions, add method for each conjunction
		if (disjunction instanceof OrPredicate) {
			OrPredicate predicate = (OrPredicate)disjunction;
			addDisjunction(conjunctions, predicate.left);
			addDisjunction(conjunctions, predicate.right);
		} else
			addConjunction(conjunctions, disjunction);
	}

	private static void addConjunction(Set<Predicate> conjunctions, Predicate conjunction) {
		if (conjunction instanceof AndPredicate)
			conjunction = ((AndPredicate)conjunction).flatten();

		// Step 7: remove all tests guaranteed to be true
		conjunction = conjunction.removeTrueAtoms();
		if (conjunction == null)
			conjunction = TruePredicate.INSTANCE;

		// Step 8: eliminate conjunctions containing
		// atomic tests  that are guaranteed to be false
		conjunction = conjunction.removeFalseConjunctions();
		if (conjunction == null)
			return;

		// Step 9: merging of duplicate conjunctions
		// (implicit through hash set)
		conjunctions.add(conjunction);
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

	public Set<Predicate> getAtoms() {
		return Collections.emptySet();
	}

	boolean implies(Predicate predicate) {
		if (this.equals(predicate)
			|| predicate instanceof TruePredicate)
		{
			return true;
		}
		if (predicate instanceof OrPredicate) {
			OrPredicate orPredicate = (OrPredicate)predicate;
			// TODO: check
			return orPredicate.left.implies(this)
				|| orPredicate.right.implies(this);
		}
		return false;
	}

	// Step 7
	public Predicate removeTrueAtoms() {
		return this;
	}

	// Step 8
	public Predicate removeFalseConjunctions() {
		return this;
	}

	boolean isAlwaysTrue() {
		return false;
	}

	boolean isAlwaysFalse() {
		return false;
	}

	void resolveTypes(Evaluator evaluator) {};

	public void compileExpressions(Compiler compiler) {};
}

abstract class BinaryPredicate extends Predicate {
	public Predicate left;
	public Predicate right;

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
	public Set<Predicate> getAtoms() {
		Set<Predicate> atoms = new LinkedHashSet<>();
		atoms.addAll(this.left.getAtoms());
		atoms.addAll(this.right.getAtoms());
		return atoms;
	}

	Predicate update(Predicate left, Predicate right) {
		if (left == null && right == null)
			return null;
		else if (left != null && right == null) {
			return left;
		} else if (left == null && right != null) {
			return right;
		} else {
			this.left = left;
			this.right = right;
			return this;
		}
	}

	@Override
	public Predicate removeTrueAtoms() {
		Predicate left = this.left.removeTrueAtoms();
		Predicate right = this.right.removeTrueAtoms();
		return update(left, right);
	}

	@Override
	public Predicate removeFalseConjunctions() {
		Predicate left = this.left.removeFalseConjunctions();
		Predicate right = this.right.removeFalseConjunctions();
		return update(left, right);
	}

	@Override
	void resolveTypes(Evaluator evaluator) {
		this.left.resolveTypes(evaluator);
		this.right.resolveTypes(evaluator);
	}

	@Override
	public void compileExpressions(Compiler compiler) {
		this.left.compileExpressions(compiler);
		this.right.compileExpressions(compiler);
	}
}
