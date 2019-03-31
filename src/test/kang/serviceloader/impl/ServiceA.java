package test.kang.serviceloader.impl;

import test.kang.serviceloader.service.IService;

// 服务实现类
public class ServiceA implements IService {
    @Override
    public String sayHello() {
        return "Hello";
    }
    
    @Override
    public String getClassName() {
        return "ServiceA";
    }
    
    @Override
    public String toString() {
        return getClassName() + "：" + sayHello();
    }
}
