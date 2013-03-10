package lila.runtime.dispatch;

import java.util.Collection;
import java.util.Iterator;

public class Utils {
	static <A, B> boolean containsAny
		(final Collection<A> a, final Collection<B> b)
	{
		for (Iterator<B> it = b.iterator(); it.hasNext();)
			if (a.contains(it.next()))
				return true;
		return false;
	}
}
