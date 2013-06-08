package lila.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.HashMap;
import java.util.Map;

public class LilaCallSite extends MutableCallSite {
	
	int chainCount = 0;

	Map<LilaMultiMethod,Map> multiMethodCache = new HashMap<>();
	
	public LilaCallSite(MethodType type) {
		super(type);
	}
	
	
	static int defaultMultiMethodCacheLimit = 2;
	static int[] multiMethodCacheLimits = new int[] {3, 5, 4};
	
	static int getMultiMethodCacheLimit(int position) {
		if (position >= multiMethodCacheLimits.length)
			return defaultMultiMethodCacheLimit;
		else 
			return multiMethodCacheLimits[position];
	}
	
	public boolean cache(LilaMultiMethod mm, LilaClass[] types, MethodHandle mh) {
//		if (multiMethodCache.size() > getMultiMethodCacheLimit(0))
//			return false;
		
		Map entry = multiMethodCache.get(mm);
		if (entry == null) {
			entry = new HashMap<>();
			multiMethodCache.put(mm, entry);
		}
		for (int i = 0; i < types.length - 1; i++) {
//			if (entry.size() > getMultiMethodCacheLimit(i + 1))
//				return false;
			
			LilaClass type = types[i];
			Map<Object, Object> currentEntry = (Map)entry.get(type);
			if (currentEntry == null) {
				currentEntry = new HashMap<>();
				entry.put(type, currentEntry);
			}
			entry = currentEntry;
		}
		int last = types.length - 1;
//		if (entry.size() > getMultiMethodCacheLimit(last + 1))
//			return false;
		
		entry.put(types[last], mh);
		
		System.out.println(multiMethodCache);
		
		return true;
	}
}