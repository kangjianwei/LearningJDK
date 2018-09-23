package test.kang.stream;

import java.util.Arrays;
import java.util.List;

// flatMap测试
public class StreamTest04 {
    public static void main(String[] args) {
        List<Integer> l1 = List.of(1,11,111);
        List<Integer> l2 = List.of(1,11,112);
        List<Integer> l3 = List.of(1,12,121);
        List<Integer> l4 = List.of(1,12,122);
        List<Integer> l5 = List.of(1,12,123);
        List<Integer> l6 = List.of(2,23,231);
        List<Integer> l7 = List.of(2,23,232);
        List<Integer> l8 = List.of(2,23,233);
        List<Integer> l9 = List.of(2,24,241);
        
        
        List<List<Integer>> ll1 = List.of(l1, l2);
        List<List<Integer>> ll2 = List.of(l3, l4, l5);
        List<List<Integer>> ll3 = List.of(l6, l7, l8);
        List<List<Integer>> ll4 = List.of(l9);
        
        
        List<List<List<Integer>>> lll1 = List.of(ll1, ll2);
        List<List<List<Integer>>> lll2 = List.of(ll3, ll4);
    
        
        List<List<List<List<Integer>>>> llll = List.of(lll1, lll2);
    
        
        // 逐级降维
        Integer[] integers = llll.stream()
            .flatMap(l->l.stream())
            .flatMap(l->l.stream())
            .flatMap(l->l.stream())
            .filter(x->x/100>0)
            .toArray(Integer[]::new);
        System.out.println(Arrays.toString(integers));
    }
}
