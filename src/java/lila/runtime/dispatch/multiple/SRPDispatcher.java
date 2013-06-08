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

	private BitSet getSet(LilaClass[] types, int pos) {
		int identifier = types[pos].getIdentifier();
		return tables.get(pos).get(identifier);
	}
	
	public MethodHandle[] dispatch(LilaClass... types) {
		BitSet set = getSet(types, 0);
		if (set == null)
			return noMethods;
		set = (BitSet)set.clone();
		for (int i = 1; i < types.length; i++) {
			BitSet otherSet = getSet(types, i);
			if (otherSet == null)
				return noMethods;
			set.and(otherSet);
		}
		MethodHandle[] result = new MethodHandle[set.cardinality()];
		for (int i = 0, j = set.nextSetBit(0); j >= 0; j = set.nextSetBit(j + 1), i++)
			result[i] = this.methods.get(j).getMethodHandle();
		return result;
	}

	private void addMethod(List<BitSet> table, LilaClass specializer, int pos) {
		for (LilaClass subtype : specializer.getAllSubclasses()) {
			int identifier = subtype.getIdentifier();
			BitSet bitset = table.get(identifier);
			if (bitset == null) {
				bitset = new BitSet();
				table.set(identifier, bitset);
			}
			bitset.set(pos);
		}
	}

	public void addNewMethod(Method method) {
		int index = Collections.binarySearch(methods, method);
		if (index < 0) 
			index = ~index;
		methods.add(index, method);
		for (int i = 0; i < tables.size(); i++) {
			ArrayList<BitSet> table = tables.get(i);
			LilaClass spezializer = method.getSpecializer(i);
			for (BitSet bitset : table) {
				if (bitset == null)
					continue;
				for (int j = bitset.length(); j > index; j--)
					bitset.set(j, bitset.get(j - 1));
				bitset.set(index, false);
			}
			this.addMethod(table, spezializer, index);
		}
	}

	public void addNewClass(LilaClass type) {
		for (int i = 0; i < tables.size(); i++) {
			ArrayList<BitSet> table = tables.get(i);
			table.add(type.getIdentifier(), null);
			int j = 0;
			for (Method method : this.methods) {
				LilaClass specializer = method.getSpecializer(i);
				if (type.isSubtypeOf(specializer))
					this.addMethod(table, type, j++);
			}
		}
	}
}
