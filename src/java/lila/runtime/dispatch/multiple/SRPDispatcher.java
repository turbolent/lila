package lila.runtime.dispatch.multiple;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import lila.runtime.LilaClass;
import lila.runtime.LilaObject;

public class SRPDispatcher {

	ArrayList<ArrayList<BitSet>> tables;
	ArrayList<Method> methods;

	public SRPDispatcher(int arity, Method... methods) {
		ArrayList<Method> allMethods = new ArrayList<>();
		Collections.addAll(allMethods, methods);
		Collections.sort(allMethods);

		Set<LilaClass> allClasses = LilaObject.lilaClass.getAllSubclasses();

		ArrayList<ArrayList<BitSet>> tables = new ArrayList<>();
		for (int i = 0; i < arity; i++) {
			ArrayList<BitSet> table = new ArrayList<>();
			for (int j = 0; j < allClasses.size(); j++)
				table.add(null);
			tables.add(table);

			int j = 0;
			for (Method method : allMethods)
				addMethod(table, method.getSpecializer(i), j++);
		}

		this.tables = tables;
		this.methods = allMethods;
	}

	static MethodHandle[] noMethods = new MethodHandle[0];

	public MethodHandle[] dispatch(LilaClass... types) {
		int identifier = types[0].getIdentifier();
		BitSet set = tables.get(0).get(identifier);
		if (set == null)
			return noMethods;
		set = (BitSet)set.clone();
		for (int i = 1; i < types.length; i++) {
			int otherIdentifier = types[i].getIdentifier();
			BitSet otherSet = tables.get(i).get(otherIdentifier);
			if (otherSet == null)
				return noMethods;
			set.and(otherSet);
		}
		MethodHandle[] result = new MethodHandle[set.cardinality()];
		int i = 0;
		for (int j = set.nextSetBit(0); j >= 0; j = set.nextSetBit(j + 1))
			result[i++] = this.methods.get(j).getMethodHandle();
		return result;
	}

	private void addMethod
		(List<BitSet> table, LilaClass specializer, int pos)
	{
		for (LilaClass subtype : specializer.getAllSubclasses()) {
			BitSet bitset = table.get(subtype.getIdentifier());
			if (bitset == null) {
				bitset = new BitSet();
				table.set(subtype.getIdentifier(), bitset);
			}
			bitset.set(pos);
		}
	}

	public void addNewMethod(Method method) {
		int index = Collections.binarySearch(methods, method);
		if (index < 0) index = ~index;

		methods.add(index, method);

		for (int i = 0; i < tables.size(); i++) {
			ArrayList<BitSet> table = tables.get(i);

			// shift all sets in table
			for (BitSet bitset : table) {
				if (bitset == null)
					continue;
				for (int j = bitset.length(); j > index; j--)
					bitset.set(j, bitset.get(j - 1));
				bitset.set(index, false);
			}

			this.addMethod(table, method.getSpecializer(i), index);
		}
	}

	public void addNewClass(LilaClass type) {
		// add new type entry to each table
		for (int i = 0; i < tables.size(); i++) {
			ArrayList<BitSet> table = tables.get(i);
			table.add(type.getIdentifier(), null);

			// add methods if applicable
			int j = 0;
			for (Method method : this.methods) {
				LilaClass specializer = method.getSpecializer(i);
				if (type.isSubtypeOf(specializer))
					this.addMethod(table, type, j++);
			}
		}
	}
}
