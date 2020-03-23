import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * @author wangheyu
 * @description: TODO
 * @date 2020/3/21
 */
public class MapUtil {

    /**
     * 取Map集合的并集
     *
     * @param map1 大集合
     * @param map2 小集合
     * @return 两个集合的并集
     */
    public static Map<String, Nod> getUnionSetByGuava(Map<String, Nod> map1, Map<String, Nod> map2) {
        Set<String> bigMapKey = map1.keySet();
        Set<String> smallMapKey = map2.keySet();
        Set<String> differenceSet = Sets.union(bigMapKey, smallMapKey);
        Map<String, Nod> result = Maps.newHashMap();
        for (String key : differenceSet) {
            if (map1.containsKey(key)) {
                result.put(key, map1.get(key));
            } else {
                result.put(key, map2.get(key));
            }
        }
        return result;
    }
    /**
     * 取Map集合的差集
     *
     * @param bigMap   大集合
     * @param smallMap 小集合
     * @return 两个集合的差集
     */
    public static Map<String, Nod> getDifferenceSetByGuava(Map<String, Nod> bigMap, Map<String, Nod> smallMap) {
        Set<String> bigMapKey = bigMap.keySet();
        Set<String> smallMapKey = smallMap.keySet();
        Set<String> differenceSet = Sets.difference(bigMapKey, smallMapKey);
        Map<String, Nod> result = Maps.newHashMap();
        for (String key : differenceSet) {
            result.put(key, bigMap.get(key));
        }
        return result;
    }
    /**
     * 取Map集合的交集（String,String）
     *
     * @param map1 大集合
     * @param map2 小集合
     * @return 两个集合的交集
     */
    public static Map<String, Nod> getIntersectionSetByGuava(Map<String, Nod> map1, Map<String, Nod> map2) {
        Set<String> bigMapKey = map1.keySet();
        Set<String> smallMapKey = map2.keySet();
        Set<String> differenceSet = Sets.intersection(bigMapKey, smallMapKey);
        Map<String, Nod> result = Maps.newHashMap();
        for (String key : differenceSet) {
            result.put(key, map1.get(key));
        }
        return result;
    }

}
