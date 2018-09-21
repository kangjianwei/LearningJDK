package test.kang.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// collect测试
public class StreamTest16 {
    public static void main(String[] args) {
        // 将字符串添加到一个list中
        Stream<String> stream1 = Stream.of("I", "am", "Chinese", "!");
        List<String> l1 = stream1.collect(() -> new ArrayList<>(), (list, e) -> list.add(e), (list1, list2) -> list1.addAll(list2));
        System.out.println(l1);
        
        // 拼接字符串
        Stream<String> stream2 = Stream.of("I ", "am ", "Chinese ", "!");
        StringBuilder concat = stream2.collect(() -> new StringBuilder(), (sb, str) -> sb.append(str), (sb1, sb2) -> sb1.append(sb2));
        System.out.println(concat);
        
        // 将字符串收集到容器中
        Stream<String> stream3 = Stream.of("I", "am", "Chinese", "!");
        List<String> l2 = stream3.collect(Collectors.toList());
        System.out.println(l2);
    }
}
