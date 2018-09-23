package test.kang.stream;

import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Collector（收集器）测试
public class StreamTest17 {
    public static void main(String[] args) {
        System.out.println("\n## 1. toCollection，自定义容器 ##");
        HashSet<Integer> myset = Stream.of(9,8,7,6,5,4,3,2,1,0)
            .collect(Collectors.toCollection(HashSet::new));
        System.out.println(myset);
    
        System.out.println("\n## 2. toList，内部使用ArrayList容器 ##");
        List<Integer> list1 = Stream.of(9,8,7,6,5,4,3,2,1,0)
            .collect(Collectors.toList());
        System.out.println(list1);
    
        System.out.println("\n## 3. toUnmodifiableList，内部使用不可变的ArrayList容器 ##");
        List<Integer> list2 = Stream.of(9,8,7,6,5,4,3,2,1,0)
            .collect(Collectors.toUnmodifiableList());
        System.out.println(list2);
    
        System.out.println("\n## 4. toSet，内部使用HashSet容器 ##");
        Set<Integer> set1 = Stream.of(9,8,7,6,5,4,3,2,1,0)
            .collect(Collectors.toSet());
        System.out.println(set1);
    
        System.out.println("\n## 5. toUnmodifiableSet，内部使用不可变的HashSet容器 ##");
        Set<Integer> set2 = Stream.of(9,8,7,6,5,4,3,2,1,0)
            .collect(Collectors.toUnmodifiableSet());
        System.out.println(set2);
    
        System.out.println("\n## 6. joining，拼接字符串 ##");
        String str1 = Stream.of("aaa", "bbb", "ccc")
            .collect(Collectors.joining());
        System.out.println(str1);
    
        System.out.println("\n## 7. joining，使用指定的分隔符拼接字符串 ##");
        String str2 = Stream.of("aaa", "bbb", "ccc")
            .collect(Collectors.joining("-"));
        System.out.println(str2);
    
        System.out.println("\n## 8. joining，使用指定的分隔符、前缀、后缀拼接字符串 ##");
        String str3 = Stream.of("aaa", "bbb", "ccc")
            .collect(Collectors.joining("-", "@", "#"));
        System.out.println(str3);
    
        System.out.println("\n## 9. filtering，自定义容器，收纳之前先过滤 ##");
        List<Integer> list3 = Stream.of(9,8,7,6,5,4,3,2,1,0)
            .collect(Collectors.filtering(x->x%2==0, Collectors.toList())); // 只保留偶数
        System.out.println(list3);
    
        System.out.println("\n## 10. mapping，自定义容器，收纳之前先映射 ##");
        List<Integer> list4 = Stream.of(9,8,7,6,5,4,3,2,1,0)
            .collect(Collectors.mapping(x->x*x, Collectors.toList())); // 元素的平方
        System.out.println(list4);
    
        System.out.println("\n## 11. flatMapping，自定义容器，收纳之前先降维 ##");
        List<Integer> list5 = Stream.of(List.of(9,8), List.of(7,6,5), List.of(4,3), List.of(2,1,0))
            .collect(Collectors.flatMapping(l->l.stream(), Collectors.toList())); // 降维
        System.out.println(list5);
    
        System.out.println("\n## 12. collectingAndThen，自定义容器，收纳之后对容器进行操作 ##");
        List<Integer> list6 = Stream.of(9,8,7,6,5,4,3,2,1,0)
            .collect(Collectors.collectingAndThen(Collectors.toList(), list-> {Collections.sort(list); return list;})); // 收纳之后对元素排序
        System.out.println(list6);
    
        System.out.println("\n## 13. counting，计数 ##");
        long count = Stream.of(9,8,7,6,5,4,3,2,1,0)
            .collect(Collectors.counting()); // 统计有多少个元素
        System.out.println(count);
    
        System.out.println("\n## 14. summingInt，int求和 ##");
        int sum1 = Stream.of("9","8","7","6","5","4","3","2","1","0")
            .collect(Collectors.summingInt(s-> Integer.parseInt(s))); // 将每个字符串解析为int并求和
        System.out.println(sum1);
    
        System.out.println("\n## 15. summingLong，long求和 ##");
        long sum2 = Stream.of("9","8","7","6","5","4","3","2","1","0")
            .collect(Collectors.summingLong(x-> Long.parseLong(x))); // 将每个字符串解析为long并求和
        System.out.println(sum2);
    
        System.out.println("\n## 16. summingDouble，double求和 ##");
        double sum3 = Stream.of("9.9","8.8","7.7","6.6","5.5","4.4","3.3","2.2","1.1","0.0")
            .collect(Collectors.summingDouble(x-> Double.parseDouble(x))); // 将每个字符串解析为double并求和
        System.out.println(sum3);
    
        System.out.println("\n## 17. averagingInt，求int的平均值 ##");
        double avg1 = Stream.of("9","8","7","6","5","4","3","2","1","0")
            .collect(Collectors.averagingInt(s-> Integer.parseInt(s))); // 将每个字符串解析为int并求和
        System.out.println(avg1);
    
        System.out.println("\n## 18. averagingLong，求long的平均值 ##");
        double avg2 = Stream.of("9","8","7","6","5","4","3","2","1","0")
            .collect(Collectors.averagingLong(s-> Long.parseLong(s))); // 将每个字符串解析为long并求和
        System.out.println(avg2);
    
        System.out.println("\n## 19. averagingDouble，求double的平均值 ##");
        double avg3 = Stream.of("9.9","8.8","7.7","6.6","5.5","4.4","3.3","2.2","1.1","0.0")
            .collect(Collectors.averagingDouble(s-> Double.parseDouble(s))); // 将每个字符串解析为double并求和
        System.out.println(avg3);
    
        System.out.println("\n## 20. minBy，求最小值 ##");
        Optional<Double> min = Stream.of(9.9,8.8,7.7,6.6,5.5,4.4,3.3,2.2,1.1,0.0)
            .collect(Collectors.minBy((a, b)-> (a-b>0 ? 1 : a-b<0 ? -1 : 0))); // 按自然顺序找出最小值
        min.ifPresent(System.out::println);
    
        System.out.println("\n## 21. maxBy，求最大值 ##");
        Optional<Double> max = Stream.of(9.9,8.8,7.7,6.6,5.5,4.4,3.3,2.2,1.1,0.0)
            .collect(Collectors.maxBy((a, b)-> (a-b>0 ? 1 : a-b<0 ? -1 : 0))); // 按自然顺序找出最小值
        max.ifPresent(System.out::println);
    
        System.out.println("\n## 22. reducing，拼接字符串，并加入分割符 ##");
        Optional<String> str4 = Stream.of("Read","The", "Fucking", "Source", "Code")
            .collect(Collectors.reducing((a, b)->(a+"-"+b))); // 按自然顺序找出最小值
        str4.ifPresent(System.out::println);
    
        System.out.println("\n## 23-1. reducing，用作返回长度最大的子串 ##");
        String str5 = Stream.of("Read","The", "Fucking", "Source", "Code")
            .collect(Collectors.reducing("", (a, b)->(a.length()>=b.length()?a:b)));
        System.out.println(str5);
    
        System.out.println("\n## 23-2. reducing，用作累加 ##");
        double d = Stream.of(9.9,8.8,7.7,6.6,5.5,4.4,3.3,2.2,1.1,0.0)
            .collect(Collectors.reducing(0.0, (a, b)->(a+b)));
        System.out.println(d);
    
        System.out.println("\n## 24. reducing，用作累加元素，累加之前将整数翻倍 ##");
        int sum4 = Stream.of(9,8,7,6,5,4,3,2,1,0)
            .collect(Collectors.reducing(0, x->2*x, (a, b)->(a+b))); // 统计有多少个元素
        System.out.println(sum4);
    
        System.out.println("\n## 25. groupingBy，将人口按城市分组 ##");
        Map<String, List<Person>> peopleByCity = Person.stream()
            .collect(Collectors.groupingBy(Person::getCity));
        for(String key : peopleByCity.keySet()){
            System.out.println(key+" "+peopleByCity.get(key));
        }
    
        System.out.println("\n## 26. groupingBy，将人口按城市分组，然后提取分组后的人口的姓名 ##");
        Map<String, Set<String>> namesByCity = Person.stream()
            .collect(Collectors.groupingBy(Person::getCity, Collectors.mapping(Person::getName, Collectors.toSet())));
        for(String key : namesByCity.keySet()){
            System.out.println(key+" "+namesByCity.get(key));
        }
    
        System.out.println("\n## 27. groupingBy，将人口按年龄分组，然后提取分组后的人口的姓名，最终的键值对存到TreeMap ##");
        Map<Integer, Set<String>> namesByAge = Person.stream()
            .collect(Collectors.groupingBy(Person::getAge, TreeMap::new, Collectors.mapping(Person::getName, Collectors.toSet())));
        for(Integer key : namesByAge.keySet()){
            System.out.println(key+" "+namesByAge.get(key));
        }
    
        System.out.println("\n## 28. groupingBy，将人口按城市分组 ##");
        Map<String, List<Person>> peopleByCity2 = Person.stream()
            .collect(Collectors.groupingByConcurrent(Person::getCity));
        for(String key : peopleByCity2.keySet()){
            System.out.println(key+" "+peopleByCity2.get(key));
        }
    
        System.out.println("\n## 29. groupingBy，将人口按城市分组，然后提取分组后的人口的姓名 ##");
        Map<String, Set<String>> namesByCity2 = Person.stream()
            .collect(Collectors.groupingByConcurrent(Person::getCity, Collectors.mapping(Person::getName, Collectors.toSet())));
        for(String key : namesByCity2.keySet()){
            System.out.println(key+" "+namesByCity2.get(key));
        }
    
        System.out.println("\n## 30. groupingBy，将人口按年龄分组，然后提取分组后的人口的姓名，最终的键值对存到ConcurrentSkipListMap ##");
        Map<Integer, Set<String>> namesByAge2 = Person.stream()
            .collect(Collectors.groupingByConcurrent(Person::getAge, ConcurrentSkipListMap::new, Collectors.mapping(Person::getName, Collectors.toSet())));
        for(Integer key : namesByAge2.keySet()){
            System.out.println(key+" "+namesByAge2.get(key));
        }
    
        System.out.println("\n## 31. partitioningBy，将人口按年龄分组，>=25岁的分一组，其他的分另一组 ##");
        Map<Boolean, List<Person>> namesByAge3 = Person.stream()
            .collect(Collectors.partitioningBy(person->person.getAge()>=25));
        for(Boolean key : namesByAge3.keySet()){
            System.out.println(key+" "+namesByAge3.get(key));
        }
    
        System.out.println("\n## 32. partitioningBy，将人口按年龄分组，>=25岁的分一组，其他的分另一组，最后显示姓名 ##");
        Map<Boolean, Set<Person>> namesByAge4 = Person.stream()
            .collect(Collectors.partitioningBy(person->person.getAge()>=25, Collectors.toSet()));
        for(Boolean key : namesByAge4.keySet()){
            System.out.println(key+" "+namesByAge4.get(key));
        }
    
        System.out.println("\n## 33. toMap，内部使用HashMap容器，key不能重复 ##");
        Map<String, String> map1 = Person.stream()
            .collect(Collectors.toMap(Person::getName, Person::getCity));
        for(String key : map1.keySet()){
            System.out.println(key+" "+map1.get(key));
        }
    
        System.out.println("\n## 34. toMap，内部使用HashMap容器，当key重复时，需要借助合并函数来合并value（所以本质上来说，key还是不重复） ##");
        Map<String, String> map2 = Person.stream()
            .collect(Collectors.toMap(Person::getCity, Person::getName, (s1, s2)->(s1+" "+s2)));
        for(String key : map2.keySet()){
            System.out.println(key+" "+map2.get(key));
        }
    
        System.out.println("\n## 35. toMap，自定义Map类容器，当key重复时，需要合并value ##");
        Map<String, String> map3 = Person.stream()
            .collect(Collectors.toMap(Person::getCity, Person::getName, (s1, s2)->(s1+" "+s2), TreeMap::new)); // 这里使用TreeMap容器
        for(String key : map3.keySet()){
            System.out.println(key+" "+map3.get(key));
        }
    
        System.out.println("\n## 36. toUnmodifiableMap，内部使用HashMap容器，key不能重复，元素放到容器后不能被修改 ##");
        Map<String, String> map4 = Person.stream()
            .collect(Collectors.toUnmodifiableMap(Person::getName, Person::getCity));
        for(String key : map4.keySet()){
            System.out.println(key+" "+map4.get(key));
        }
    
        System.out.println("\n## 37. toUnmodifiableMap，当key重复时，需要合并value，元素放到容器后不能被修改 ##");
        Map<String, String> map5 = Person.stream()
            .collect(Collectors.toUnmodifiableMap(Person::getCity, Person::getName, (s1, s2)->(s1+" && "+s2)));
        for(String key : map5.keySet()){
            System.out.println(key+" "+map5.get(key));
        }
    
        System.out.println("\n## 38. toConcurrentMap，使用ConcurrentHashMap容器，key不能重复 ##");
        Map<String, String> map8 = Person.stream()
            .collect(Collectors.toConcurrentMap(Person::getName, Person::getCity));
        for(String key : map8.keySet()){
            System.out.println(key+" "+map8.get(key));
        }
    
        System.out.println("\n## 39. toConcurrentMap，使用ConcurrentHashMap容器，当key重复时，需要合并value ##");
        Map<String, String> map6 = Person.stream()
            .collect(Collectors.toConcurrentMap(Person::getCity, Person::getName, (s1, s2)->(s1+" -- "+s2)));
        for(String key : map6.keySet()){
            System.out.println(key+" "+map6.get(key));
        }
    
        System.out.println("\n## 40. toConcurrentMap，自定义ConcurrentMap类容器，当key重复时，需要合并value ##");
        Map<Integer, String> map7 = Person.stream()
            .collect(Collectors.toConcurrentMap(Person::getAge, Person::getName, (s1, s2)->(s1+" ## "+s2), ConcurrentSkipListMap::new)); // 这里使用TreeMap容器
        for(Integer key : map7.keySet()){
            System.out.println(key+" "+map7.get(key));
        }
    
        System.out.println("\n## 41. summarizingInt，对int类型的元素统计相关信息：计数、求和、均值、最小值、最大值 ##");
        IntSummaryStatistics is1 = Stream.of("9","8","7","6","5","4","3","2","1","0")
            .collect(Collectors.summarizingInt(s-> Integer.parseInt(s)));
        System.out.println(is1);
    
        System.out.println("\n## 42. summarizingLong，对long类型的元素统计相关信息：计数、求和、均值、最小值、最大值 ##");
        LongSummaryStatistics is2 = Stream.of("9","8","7","6","5","4","3","2","1","0")
            .collect(Collectors.summarizingLong(s-> Long.parseLong(s)));
        System.out.println(is2);
    
        System.out.println("\n## 43. summarizingDouble，对double类型的元素统计相关信息：计数、求和、均值、最小值、最大值 ##");
        DoubleSummaryStatistics is3 = Stream.of("9.9","8.8","7.7","6.6","5.5","4.4","3.3","2.2","1.1","0.0")
            .collect(Collectors.summarizingDouble(s-> Double.parseDouble(s)));
        System.out.println(is3);
    }
    
    static class Person {
        private String city;
        private String name;
        private int age;
    
        private static List<Person> people = List.of(
            new Person("City-A","张三", 20),
            new Person("City-A","李四", 25),
            new Person("City-B","王五", 28),
            new Person("City-A","赵六", 20),
            new Person("City-C","孙七", 20),
            new Person("City-B","钱八", 23),
            new Person("City-C","周九", 28)
        );
    
        Person(String city, String name, int age) {
            this.city = city;
            this.name = name;
            this.age = age;
        }
    
        static Stream<Person> stream(){
            return people.stream();
        }
    
        public String getCity() {
            return city;
        }
    
        public String getName() {
            return name;
        }
    
        public int getAge() {
            return age;
        }
    
        @Override
        public String toString() {
            return "Person{" + "city='" + city + '\'' + ", name='" + name + '\'' + ", age=" + age + '}';
        }
    }
}
