package test.kang.thread;

import java.lang.Thread.UncaughtExceptionHandler;

// 未捕获异常
public class ThreadTest05 {
    public static void main(String[] args) {
        // 初始化一个未捕获异常处理器
        UncaughtExceptionHandler handler = new MyHandler();
        
        // 注册未捕获异常处理器
        Thread.setDefaultUncaughtExceptionHandler(handler);
        
        // 这里出现了除0的未捕获异常，会被MyHandler捕获到
        int i = 1/0;
    }
}

// 处理未捕获异常
class MyHandler implements Thread.UncaughtExceptionHandler{
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        System.out.println("线程"+t+"内出现了未捕获异常："+e);
    }
}
