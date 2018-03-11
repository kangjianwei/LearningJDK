package test.kang.unsafe;

import java.util.Arrays;
import sun.misc.Unsafe;

public class Entry {
    public Unsafe unsafe = UnsafeUtil.getUnsafeInstance();
    
    private static boolean booConst = false;  // 常量
    
    private String strVar = "唐僧";
    private char[] charArray = new char[]{'东','土','大', '唐'};
    private int intVar = 123;
    private double doubleVar = 1.23;
    
    public Entry() {
    }
    
    public Entry(String strVar, char[] charArray, int intVar, double doubleVar) {
        this.strVar = strVar;
        this.charArray = charArray;
        this.intVar = intVar;
        this.doubleVar = doubleVar;
    }
    
    @Override
    public String toString() {
        return "Entry{" + "strVar='" + strVar + '\'' + ", charArray=" + Arrays.toString(charArray) + ", intVar=" + intVar + ", doubleVar=" + doubleVar + '}';
    }
    
    public void setInfo(String strVar, char[] charArray, int intVar, double doubleVar) {
        this.strVar = strVar;
        this.charArray = charArray;
        this.intVar = intVar;
        this.doubleVar = doubleVar;
    }
    
    public char[] getCharArray() {
        return charArray;
    }
}
