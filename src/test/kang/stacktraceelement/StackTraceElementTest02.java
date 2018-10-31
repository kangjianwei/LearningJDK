package test.kang.stacktraceelement;

import java.util.Map;
import java.util.Map.Entry;

// 当前JVM内所有线程的栈帧
public class StackTraceElementTest02 {
    public static void main(String[] args) {
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i=0; i<1000; i++){
                    // 空循环，让线程t1保持活跃
                }
            }
        }, "子线程1");
    
        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i=0; i<1000; i++){
                    // 空循环，让线程t2保持活跃
                }
            }
        }, "子线程2");
    
        t1.start();
        t2.start();
    
        Map<Thread, StackTraceElement[]> stackTraceElements = Thread.getAllStackTraces();
        for(Entry<Thread, StackTraceElement[]> entry : stackTraceElements.entrySet()){
            System.out.println(entry.getKey());
            for(StackTraceElement ste : entry.getValue()){
                System.out.println("    "+ste);
            }
        }
    }
}
