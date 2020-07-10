package java.net;

/**
 * Simple factory to create the impl
 */
// InetAddressImpl对象工厂
class InetAddressImplFactory {
    
    // 创建InetAddressImpl的实现类（分为IP4和IP6两种）
    static InetAddressImpl create() {
        return InetAddress.loadImpl(isIPv6Supported() ? "Inet6AddressImpl" : "Inet4AddressImpl");
    }
    
    // 系统是否支持IP6协议
    static native boolean isIPv6Supported();
    
}
