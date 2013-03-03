package lila.runtime.dispatch;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

class Utils {
	static <A, B> boolean containsAny
		(final Collection<A> a, final Collection<B> b)
	{
		for (Iterator<B> it = b.iterator(); it.hasNext();)
			if (a.contains(it.next()))
				return true;
		return false;
	}

	static void dumpMethods(Map<Predicate,Method> methods) {
		StringBuilder builder = new StringBuilder();
		for (Entry<Predicate, Method> entry : methods.entrySet()) {
			builder.append(String.format(	"\n  when %s %s", entry.getKey(),
											entry.getValue()));
		}
		System.err.println("GF" + builder);
	}
}
