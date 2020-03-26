package com.process.utils;

import com.process.entity.Nod;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author wangheyu
 * @description: TODO
 * @date 2020/3/21
 */
public class MapUtil {

    /**
     * 取Map集合的交集（String,String）
     *
     * @param map1 大集合
     * @param map2 小集合
     * @return 两个集合的交集
     */
    public static Map<Integer, Nod> getIntersectionSetByGuava(Map<Integer, Nod> map1, Map<Integer, Nod> map2) {
        Set<Integer> bigMapKey = map1.keySet();
        Set<Integer> smallMapKey = map2.keySet();
        Map<Integer, Nod> result = new HashMap<>();
        bigMapKey.stream().forEach(m1->{
            smallMapKey.stream().forEach(m2->{
                if(m1.equals(m2)){
                    result.put(m1,map1.get(m1));
                }
            });
        });
        return result;
    }

}
