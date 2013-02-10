package lila.runtime;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class C3 {

	private static <T> List<T> tail(List<T> l) {
		int size = l.size();
		return (size > 1
				? l.subList(1, size)
				: Collections.<T>emptyList());
	}

	private static LilaClass candidate
		(List<List<LilaClass>> remainingInputs, LilaClass c)
	{
		for (List<LilaClass> classes : remainingInputs)
			if (tail(classes).contains(c))
				return null;
		return c;
	}

	private static LilaClass candidateAtHead
		(List<List<LilaClass>> remainingInputs, List<LilaClass> classes)
	{
		return (classes.isEmpty()
				? null
				: candidate(remainingInputs, classes.get(0)));
	}

	private static List<LilaClass> removeNext
		(LilaClass next, List<LilaClass> classes)
	{
		if (!classes.isEmpty()
			&& classes.get(0) == next)
			return tail(classes);
		else
			return classes;
	}

	private static List<LilaClass> mergeLists
		(List<LilaClass> reversedPartialResult,
		 List<List<LilaClass>> remainingInputs)
	{
		boolean allRemainingInputsEmpty = true;
		for (List<LilaClass> remainingInput : remainingInputs) {
			if (!remainingInput.isEmpty()) {
				allRemainingInputsEmpty = false;
				break;
			}
		}

		if (allRemainingInputsEmpty) {
			Collections.reverse(reversedPartialResult);
			return reversedPartialResult;
		} else {

			LilaClass next = null;
			for (List<LilaClass> remainingInput : remainingInputs) {
				next = candidateAtHead(remainingInputs, remainingInput);
				if (next != null)
					break;
			}

			if (next == null)
				throw new Error("Inconsistent precedence graph");
			else {
				LinkedList<LilaClass> partialResult = new LinkedList<>();
				partialResult.add(next);
				partialResult.addAll(reversedPartialResult);

				LinkedList<List<LilaClass>> remaining = new LinkedList<>();
				for (List<LilaClass> remainingInput : remainingInputs)
					remaining.add(removeNext(next, remainingInput));

				return mergeLists(partialResult, remaining);
			}
		}
	}

	private static List<LilaClass> cplList(LilaClass c) {
		// NOTE: assumes getAllSuperclasses
		// is using C3's computeClassLinearization
		return new LinkedList<>(c.getAllSuperclasses());
	}

	public static List<LilaClass> computeClassLinearization(LilaClass c) {

		LilaClass[] cDirectSuperclasses = c.getDirectSuperclasses();

		LinkedList<LilaClass> partialResult = new LinkedList<>();
		partialResult.add(c);

		LinkedList<List<LilaClass>> remaining = new LinkedList<>();
		for (LilaClass cDirectSuperclass : cDirectSuperclasses)
			remaining.add(cplList(cDirectSuperclass));
		remaining.add(Arrays.asList(cDirectSuperclasses));

		return mergeLists(partialResult, remaining);
	}
}
