package test.kang.reference;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

// 继承了弱引用，包含了一个不受追踪的强引用key
public class CustomReference<T> extends WeakReference<T> {
    // 执行GC后，弱引用本身与引用key不会被回收，只有被弱引用包裹的value会被回收
    private IDCard key;
    
    public CustomReference(IDCard key, T value) {
        super(value);
        this.key = key;
    }
    
    public CustomReference(IDCard key, T value, ReferenceQueue<? super T> q) {
        super(value, q);
        this.key = key;
    }
    
    public IDCard getKey() {
        return key;
    }
}
