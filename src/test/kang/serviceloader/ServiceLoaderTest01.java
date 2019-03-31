package test.kang.serviceloader;

import java.util.ServiceLoader;
import test.kang.serviceloader.service.IService;

/*
 * 加载服务之前，要确保服务已经注册
 * 注册文件固定位于{class根目录}/META-INF/services目录下
 */
public class ServiceLoaderTest01 {
    public static void main(String[] args) {
        // 加载服务（懒加载）
        ServiceLoader<IService> serviceLoader = ServiceLoader.load(IService.class);
        
        // 使用服务
        for(IService service : serviceLoader) {
            System.out.println(service);
        }
    }
}
