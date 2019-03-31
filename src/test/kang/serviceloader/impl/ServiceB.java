package test.kang.serviceloader.impl;

import test.kang.serviceloader.service.IService;

// 服务实现类
public class ServiceB implements IService {
    @Override
    public String sayHello() {
        return "你好";
    }
    
    @Override
    public String getClassName() {
        return "ServiceB";
    }
    
    @Override
    public String toString() {
        return getClassName() + "：" + sayHello();
    }
}
