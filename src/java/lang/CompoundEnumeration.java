package java.lang;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * A utility class that will enumerate over an array of enumerations.
 */
/*
 * 复合枚举类型
 *
 * 其内部定义了Enumeration数组字段，其自身又是Enumeration的子类
 * 这意味着该类型可以嵌套存储Enumeration类型
 */
final class CompoundEnumeration<E> implements Enumeration<E> {
    private final Enumeration<E>[] enums;
    private int index;  // 游标，用于遍历元素
    
    public CompoundEnumeration(Enumeration<E>[] enums) {
        this.enums = enums;
    }
    
    private boolean next() {
        while (index < enums.length) {
            if (enums[index] != null && enums[index].hasMoreElements()) {
                return true;
            }
            index++;
        }
        return false;
    }
    
    // 是否存在未遍历的元素
    public boolean hasMoreElements() {
        return next();
    }
    
    // 返回下一个元素
    public E nextElement() {
        if (!next()) {
            throw new NoSuchElementException();
        }
        return enums[index].nextElement();
    }
}
