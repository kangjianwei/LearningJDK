package test.kang.serializable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

// 序列化工具类
public class SerializableUtil {
    // 序列化User对象到file文件中
    public static void serializableUser(Object obj, File file){
        try {
            ObjectOutputStream oos =  new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(obj);
            oos.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    // 从file文件中反序列化User对象
    public static Object deserializableUser(File file){
        Object user = null;
        
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            user = (Object) ois.readObject();
        } catch(IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        
        return user;
    }
}
