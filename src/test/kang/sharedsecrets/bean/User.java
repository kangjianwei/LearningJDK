package test.kang.sharedsecrets.bean;


import test.kang.sharedsecrets.internal.misc.AccessBean;
import test.kang.sharedsecrets.internal.misc.SharedSecrets;

public class User {
    
    static {
        // 留下后门，以便对非public方法fun1和fun2的方法
        SharedSecrets.setAccessBean(new AccessBean() {
            @Override
            public void fun1(User user) {
                user.fun1();
            }
            
            @Override
            public void fun2(User user) {
                user.fun2();
            }
        });
    }
    
    private void fun1() {
        System.out.println("private方法");
    }
    
    void fun2() {
        System.out.println("default方法");
    }
    
    protected void fun3() {
        System.out.println("protected方法");
    }
    
    public void fun4() {
        System.out.println("public方法");
    }
}
