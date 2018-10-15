package test.kang.system;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

// 日志
public class SystemTest03 {
    public static void main(String[] args) {
        Logger logger = System.getLogger("SystemTest03");
        System.out.println(logger.getName());
        
        logger.log(Level.INFO, "Level.INFO 级别的日志");
        logger.log(Level.WARNING, "Level.WARNING 级别的日志");
    }
}
