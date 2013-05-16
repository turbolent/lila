package lila.runtime.dispatch.predicate;

import static java.lang.invoke.MethodType.methodType;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.V1_7;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import lila.runtime.DynamicClassLoader;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

abstract class DispatchTreeNode {

	abstract LookupDAGNode evaluate(int identifier);

	abstract MethodHandle compileHandle();
	// standalone
	abstract void compileASMHandle(MethodVisitor mv, List<LookupDAGNode> targets);

	abstract void compileASM(MethodVisitor mv, int argumentCount);


	// Debugging

	String name = "n" + count++;
	static int count = 1;

	abstract void dump(Writer out) throws IOException;

}

class DispatchTreeLeafNode extends DispatchTreeNode {
	LookupDAGNode targetNode;
	public DispatchTreeLeafNode(LookupDAGNode targetNode) {
		this.targetNode = targetNode;
	}

	@Override
	LookupDAGNode evaluate(int identifier) {
		return this.targetNode;
	}

	@Override
	MethodHandle compileHandle() {
		MethodHandle valueMethodHandle =
			MethodHandles.constant(LookupDAGNode.class, this.targetNode);
		return MethodHandles.dropArguments(valueMethodHandle, 0, int.class);
	}


	@Override
	void compileASMHandle(MethodVisitor mv, List<LookupDAGNode> targets) {
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitLdcInsn(targets.size());
		mv.visitInsn(Opcodes.AALOAD);
		targets.add(targetNode);
	}

	@Override
	void compileASM(MethodVisitor mv, int argumentCount) {
		this.targetNode.compileASM(mv, argumentCount);
	}

	// Debugging

	void dump(Writer out) throws IOException {
		out.write(String.format("%s [shape=box,label=\"%s\"];\n",
		                        this.name, this.targetNode));
	}
}


class DispatchTreeInteriorNode extends DispatchTreeNode {
	DispatchTreeTest test;
	DispatchTreeNode trueNode;
	DispatchTreeNode falseNode;

	public DispatchTreeInteriorNode
		(DispatchTreeTest test, DispatchTreeNode trueNode, DispatchTreeNode falseNode)
	{
		this.trueNode = trueNode;
		this.falseNode = falseNode;
		this.test = test;
	}


	@Override
	LookupDAGNode evaluate(int identifier) {
		return this.test.evaluate(identifier)
			? this.trueNode.evaluate(identifier)
			: this.falseNode.evaluate(identifier);
	}

	@Override
	MethodHandle compileHandle() {
		return MethodHandles.guardWithTest(this.test.compileHandle(),
		                                   this.trueNode.compileHandle(),
		                                   this.falseNode.compileHandle());
	}

	@Override
	void compileASMHandle(MethodVisitor mv, List<LookupDAGNode> targets) {
		Label elseLabel = new Label();
		this.test.compileASMHandle(mv, elseLabel);
		Label endLabel = new Label();
	    this.trueNode.compileASMHandle(mv, targets);
	    mv.visitJumpInsn(Opcodes.GOTO, endLabel);
	    mv.visitLabel(elseLabel);
	    this.falseNode.compileASMHandle(mv, targets);
	    mv.visitLabel(endLabel);
	}

	@Override
	void compileASM(MethodVisitor mv, int argumentCount) {
		Label elseLabel = new Label();
		this.test.compileASM(mv, elseLabel);
		Label endLabel = new Label();
	    this.trueNode.compileASM(mv, argumentCount);
	    mv.visitJumpInsn(Opcodes.GOTO, endLabel);
	    mv.visitLabel(elseLabel);
	    this.falseNode.compileASM(mv, argumentCount);
	    mv.visitLabel(endLabel);
	}

	// Debugging

	boolean dumped = false;

	void dump(Writer out) throws IOException {

		if (dumped)
			return;
		dumped = true;

		out.write(String.format("%s [label=\"%s\"];\n",
		                        this.name, this.test));
		this.trueNode.dump(out);
		this.falseNode.dump(out);
		out.write(String.format("%s -> %s [label=\"true\"];\n",
		                        this.name, this.trueNode.name));
		out.write(String.format("%s -> %s [label=\"false\"];\n",
		                        this.name, this.falseNode.name));
	}

}

class DispatchTreeMapNode extends DispatchTreeNode {
	Map<Integer,LookupDAGNode> map;

	public DispatchTreeMapNode
		(Map<Integer,LookupDAGNode> map)
	{
		this.map = map;
	}

	static LookupDAGNode lookup(Map<Integer,LookupDAGNode> map, int identifier) {
		return map.get(identifier);
	}

	static MethodHandle lookupHandle;

	static Lookup lookup = MethodHandles.lookup();
	static {
		try {
			lookupHandle = lookup.findStatic(DispatchTreeMapNode.class, "lookup",
			                                 methodType(LookupDAGNode.class,
			                                            Map.class, int.class));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	@Override
	LookupDAGNode evaluate(int identifier) {
		return lookup(this.map, identifier);
	}

	@Override
	MethodHandle compileHandle() {
		return lookupHandle.bindTo(this.map);
	}

	@Override
	void compileASMHandle(MethodVisitor mv, List<LookupDAGNode> targets) {
		// Unable to compile DispatchTreeMapNode to ASM
	}

	@Override
	void compileASM(MethodVisitor mv, int argumentCount) {
		// Unable to compile DispatchTreeMapNode to ASM
	}


	// Debugging

	static int leafCount = 1;

	boolean dumped = false;

	void dump(Writer out) throws IOException {

		if (dumped)
			return;
		dumped = true;

		out.write(String.format("%s [label=\"map\"];\n", this.name));
		for (Entry<Integer,LookupDAGNode> entry : this.map.entrySet()) {
			String leafName = "x" + leafCount++;
			out.write(String.format("%s [shape=box,label=\"%s\"];\n",
			                        leafName, entry.getValue()));
			out.write(String.format("%s -> %s [label=\"%s\"];\n",
			                        this.name, leafName, entry.getKey()));
		}
	}

}

abstract class DispatchTreeTest {
	int identifier;
	public DispatchTreeTest(int identifier) {
		this.identifier = identifier;
	}

	abstract boolean evaluate(int identifier);

	abstract MethodHandle getComparison();

	MethodHandle compileHandle() {
		return MethodHandles.insertArguments(getComparison(), 0, this.identifier);
	}

	void compileASMHandle(MethodVisitor mv, Label elseLabel) {
		mv.visitVarInsn(Opcodes.ILOAD, 1);
		mv.visitLdcInsn(this.identifier);
		mv.visitJumpInsn(getElseJumpOpcode(), elseLabel);
	}

	void compileASM(MethodVisitor mv, Label elseLabel) {
		// duplicate type identifier
		mv.visitInsn(Opcodes.DUP);
		mv.visitLdcInsn(this.identifier);
		mv.visitJumpInsn(getElseJumpOpcode(), elseLabel);
	}


	abstract int getElseJumpOpcode();

}

class DispatchTreeEqualTest extends DispatchTreeTest {
	public DispatchTreeEqualTest(int identifier) {
		super(identifier);
	}

	@Override
	boolean evaluate(int identifier) {
		return compare(this.identifier, identifier);
	}

	@Override
	public String toString() {
		return "= " + this.identifier;
	}

	// first argument will be bound to identifier
	static boolean compare(int id1, int id2) {
		return id2 == id1;
	}

	static MethodHandle compare;

	static Lookup lookup = MethodHandles.lookup();
	static {
		try {
			compare = lookup.findStatic(DispatchTreeEqualTest.class, "compare",
			                            methodType(boolean.class, int.class, int.class));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	MethodHandle getComparison() {
		return compare;
	}

	@Override
	int getElseJumpOpcode() {
		return Opcodes.IF_ICMPNE;
	}
}

class DispatchTreeLessTest extends DispatchTreeTest {
	public DispatchTreeLessTest(int identifier) {
		super(identifier);
	}

	@Override
	boolean evaluate(int identifier) {
		return compare(this.identifier, identifier);
	}

	@Override
	public String toString() {
		return "< " + this.identifier;
	}

	// first argument will be bound to identifier
	static boolean compare(int id1, int id2) {
		return id2 < id1;
	}

	static MethodHandle compare;

	static Lookup lookup = MethodHandles.lookup();
	static {
		try {
			compare = lookup.findStatic(DispatchTreeLessTest.class, "compare",
			                            methodType(boolean.class, int.class, int.class));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	MethodHandle getComparison() {
		return compare;
	}

	@Override
	int getElseJumpOpcode() {
		return Opcodes.IF_ICMPGE;
	}
}



class Divider {
	int identifier;
	int frequencyBelow;
	int frequencyAbove;

	public Divider(int identifier, int frequencyBelow, int frequencyAbove) {
		this.identifier = identifier;
		this.frequencyBelow = frequencyBelow;
		this.frequencyAbove = frequencyAbove;
	}

	@Override
	public String toString() {
		return String.format("#<Divider %d (%d, %d)>", this.identifier,
		                     this.frequencyBelow, this.frequencyAbove);
	}
}

public class DispatchTreeBuilder {

	static double threshold = 0.4;

	private Map<Integer,Integer> frequencies;

	private int getFrequency(int identifier) {
		if (frequencies != null) {
			Integer frequency = this.frequencies.get(identifier);
			if (frequency != null)
				return frequency;
		}
		return 1;
	}

	private List<Integer> sortedIdentifiers(Map<Integer,?> map) {
		List<Integer> result = new ArrayList<>();
		result.addAll(map.keySet());
		Collections.sort(result, new Comparator<Integer>() {
			@Override
			public int compare(Integer id1, Integer id2) {
				return getFrequency(id1) > getFrequency(id2) ? -1 : 1;
			}
		});
		return result;
	}

	//
	public DispatchTreeNode buildDispatchTree
		(LookupDAGInteriorNode node, Map<Integer,Integer> frequencies)
	{
		this.frequencies = frequencies;
		Map<Integer,LookupDAGNode> map = new HashMap<>();
		for (LookupDAGEdge edge : node.edges) {
			map.put(edge.type.getIdentifier(), edge.targetNode);
		}
		List<Integer> sortedIdentifiers = sortedIdentifiers(map);
		int totalFrequency = 0;
		for (int identifier : map.keySet())
			totalFrequency += getFrequency(identifier);
		return buildDispatchSubTree(map, sortedIdentifiers, totalFrequency);
	}

	private DispatchTreeNode buildDispatchSubTree
		(Map<Integer,LookupDAGNode> map, List<Integer> sortedIdentifiers, int totalFrequency)
	{
//		return new DispatchTreeMapNode(map);

		// (1)
		Set<LookupDAGNode> targets = new HashSet<>();
		targets.addAll(map.values());
		if (targets.size() == 1) {
			return new DispatchTreeLeafNode(targets.iterator().next());
		}
		// (2)
		int bestIdentifier = sortedIdentifiers.get(0);
		if (getFrequency(bestIdentifier) > totalFrequency * threshold) {
			LookupDAGNode bestTargetNode = map.get(bestIdentifier);
			DispatchTreeNode trueNode = new DispatchTreeLeafNode(bestTargetNode);

			map.remove(bestIdentifier);
			sortedIdentifiers.remove((Integer)bestIdentifier);
			totalFrequency -= getFrequency(bestIdentifier);
			DispatchTreeNode falseNode =
				buildDispatchSubTree(map, sortedIdentifiers, totalFrequency);

			DispatchTreeTest test = new DispatchTreeEqualTest(bestIdentifier);
			return new DispatchTreeInteriorNode(test, trueNode, falseNode);
		}
		// (3)
		Divider divider = pickDivider(map);
		Map<Integer,LookupDAGNode> trueMap = new HashMap<>();
		Map<Integer,LookupDAGNode> falseMap = new HashMap<>();
		for (Entry<Integer,LookupDAGNode> entry : map.entrySet()) {
			Integer identifier = entry.getKey();
			Map<Integer,LookupDAGNode> targetMap = (identifier < divider.identifier
													? trueMap
													: falseMap);
			targetMap.put(identifier, entry.getValue());
		}
		List<Integer> trueSortedIdentifiers = sortedIdentifiers(trueMap);
		List<Integer> falseSortedIdentifiers = sortedIdentifiers(falseMap);
		DispatchTreeNode trueNode = buildDispatchSubTree(trueMap, trueSortedIdentifiers,
		                                                 divider.frequencyBelow);
		DispatchTreeNode falseNode = buildDispatchSubTree(falseMap, falseSortedIdentifiers,
		                                                  divider.frequencyAbove);
		DispatchTreeTest test = new DispatchTreeLessTest(divider.identifier);
		return new DispatchTreeInteriorNode(test, trueNode, falseNode);

	}

	private Divider pickDivider(Map<Integer, LookupDAGNode> map) {
		int bestFrequencyDifference = Integer.MAX_VALUE;
		Integer bestFrequencyBelow = null;
		Integer bestFrequencyAbove = null;
		Integer bestIdentifier = null;
		for (Integer identifier : map.keySet()) {
			if (identifier == bestIdentifier)
				continue;

			int frequencyBelow = 0;
			int frequencyAbove = 0;
			for (Integer otherIdentifier : map.keySet()) {
				int frequency = getFrequency(otherIdentifier);
				if (otherIdentifier < identifier)
					frequencyBelow += frequency;
				else
					frequencyAbove += frequency;
			}
			int frequencyDifference = Math.abs(frequencyBelow - frequencyAbove);
			if (frequencyDifference <= bestFrequencyDifference) {
				bestFrequencyDifference = frequencyDifference;
				bestFrequencyBelow = frequencyBelow;
				bestFrequencyAbove = frequencyAbove;
				bestIdentifier = identifier;
			}
		}
		return new Divider(bestIdentifier, bestFrequencyBelow, bestFrequencyAbove);
	}



	private static MethodType dispatcherType = methodType(LookupDAGNode.class,
	                                                    LookupDAGNode[].class, int.class);

	public static MethodHandle compileASMHandle(DispatchTreeNode node) throws Throwable {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES |
			ClassWriter.COMPUTE_MAXS);

		String name = "Dispatcher";

		// class
		cw.visit(V1_7, ACC_PUBLIC | ACC_SUPER, name,
				null, "java/lang/Object", null);

		String dispatcherDescriptor = dispatcherType.toMethodDescriptorString();

		// add method
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "dispatch",
		                                  dispatcherDescriptor, null, null);

		List<LookupDAGNode> targets = new ArrayList<>();
		mv.visitCode();
		node.compileASMHandle(mv, targets);
		mv.visitInsn(Opcodes.ARETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
		cw.visitEnd();

		System.err.println(">>> " + targets);

		DynamicClassLoader loader = new DynamicClassLoader();
		byte[] code = cw.toByteArray();
		FileOutputStream f;
		try {
			f = new FileOutputStream("dispatch.class");
			f.write(code);
			f.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		Class<?> clazz = loader.define(name, code);
		return loader.findMethod(clazz, "dispatch", dispatcherType)
		             .bindTo(targets.toArray(new LookupDAGNode[targets.size()]));
	}

	public static void dump(DispatchTreeNode node) throws Exception {
		FileWriter fileWriter = new FileWriter("dispatchTree.dot");
		BufferedWriter writer = new BufferedWriter(fileWriter);
		writer.write("digraph dispatchTree {\n");
		writer.write("rankdir=TB; ranksep=1; nodesep=0.7; forcelabels=true;\n");
		writer.write("node [fontsize=10]; edge [fontsize=10];\n");
		node.dump(writer);
		writer.write("\n}");
		writer.close();
		fileWriter.close();
	}
}
