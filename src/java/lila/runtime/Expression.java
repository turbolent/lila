package lila.runtime;

import java.util.Set;

import lila.runtime.dispatch.PredicateEnvironment;

public abstract class Expression {
	// TODO: replace with compilation
	public abstract LilaObject evaluate(ExpressionEnvironment env);

	public abstract Expression resolveBindings(PredicateEnvironment env);

	// Static information

	public Set<LilaClass> staticClasses;

	public Set<LilaClass> getStaticClasses() {
		return (this.staticClasses == null
				? LilaObject.lilaClass.getAllSubclasses()
				: this.staticClasses);
	}

	public long cost = -1;

	public Long getCost() {
		return (this.cost == -1
			 	? Long.MAX_VALUE
			 	: this.cost);
	}

	// Debugging

	public String name;
}
