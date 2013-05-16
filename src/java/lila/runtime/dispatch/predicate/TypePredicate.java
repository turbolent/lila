package lila.runtime.dispatch.predicate;

import java.util.HashSet;
import java.util.Set;

import lila.runtime.Evaluator;
import lila.runtime.Expression;
import lila.runtime.LilaClass;
import lila.runtime.LilaNegatedClass;
import lila.runtime.LilaObject;
import lila.runtime.Compiler;

public class TypePredicate extends Predicate {

	public Expression expression;

	// temporarily holds type expression at
	// parse time until evaluated by interpreter
	public Expression typeExpression;
	LilaClass type;

	public TypePredicate(Expression expression, LilaClass type) {
		this.expression = expression;
		this.type = type;
	}

	@Override
	boolean implies(Predicate predicate) {
		if (super.implies(predicate))
			return true;
		if (predicate instanceof TypePredicate) {
			TypePredicate p = (TypePredicate) predicate;
			return p.expression.equals(this.expression)
				&& p.type.getAllSubclasses().contains(this.type);
		}
		return false;
	}

	@Override
	Predicate prepareForDNF(PredicateEnvironment env) {
		this.expression = this.expression.resolveBindings(env);
		return this;
	}

	public Set<Predicate> getAtoms() {
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
	public Predicate removeTrueAtoms() {
		return this.isAlwaysTrue() ? null : this;
	}

	@Override
	boolean isAlwaysTrue() {
		if (this.type instanceof LilaNegatedClass) {
			Set<LilaClass> classes = new HashSet<>();
			classes.addAll(this.expression.getStaticClasses());
			LilaNegatedClass negatedType = (LilaNegatedClass)this.type;
			classes.retainAll(negatedType.getType().getAllSubclasses());
			return classes.isEmpty();
		} else
			return this.type.getAllSubclasses()
				.containsAll(this.expression.getStaticClasses());
	}

	@Override
	boolean isAlwaysFalse() {
		if (this.type instanceof LilaNegatedClass) {
			LilaNegatedClass negatedType = (LilaNegatedClass)this.type;
			return negatedType.getType().getAllSubclasses()
				.containsAll(this.expression.getStaticClasses());
		} else {
			Set<LilaClass> classes = new HashSet<>();
			classes.addAll(this.expression.getStaticClasses());
			classes.retainAll(this.type.getAllSubclasses());
			return classes.isEmpty();
		}
	}

	@Override
	void resolveTypes(Evaluator evaluator) {
		LilaObject result = evaluator.evaluate(this.typeExpression);
		if (result instanceof LilaClass)
			this.type = (LilaClass)result;
		else
			throw new RuntimeException("Type expression in type predicate "
			                           + "should evaluate to a class: "
			                           + this.typeExpression.toString()
			                           + " = " + result);
	}

	@Override
	public void compileExpressions(Compiler compiler) {
		compiler.compile(this.expression);
	}

	@Override
	public String toString() {
		return String.format("%s@%s", this.expression.toString(), this.type);
	}
}

