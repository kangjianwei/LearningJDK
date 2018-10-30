package test.kang.stacktraceelement;

// 当前线程栈帧
public class StackTraceElementTest01 {
    private StackTraceElement[] stackTraceElements; // 当前线程中的栈帧
    
    public StackTraceElementTest01() {
        // 当前线程
        Thread currentThread = Thread.currentThread();
        stackTraceElements = currentThread.getStackTrace();
    }
    
    public static void main(String[] args) {
        StackTraceElementTest01 stet = new StackTraceElementTest01();
        
        System.out.println("\n-----------打印方法调用链的栈帧-----------");
        stet.fun1();
        
        System.out.println("\n-----------查看main方法的栈帧信息（位于栈底）-----------");
        StackTraceElement ste = stet.stackTraceElements[stet.stackTraceElements.length-1];
        System.out.println(ste);
        System.out.println(ste.getModuleName());
        System.out.println(ste.getClassName());
        System.out.println(ste.getMethodName());
        System.out.println(ste.getFileName());
        System.out.println(ste.getLineNumber());
        System.out.println(ste.getClassLoaderName());
        System.out.println(ste.getModuleVersion());
    
        try {
            Thread.sleep(1000);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private void fun1(){
        fun2();
    }
    
    private void fun2(){
        for(StackTraceElement ste : stackTraceElements){
            System.out.println(ste);
        }
    }
}
