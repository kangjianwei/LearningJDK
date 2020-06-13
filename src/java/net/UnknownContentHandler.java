package java.net;

import java.io.IOException;

// 未知类型的资源句柄，通常以字节流形式返回资源内容
class UnknownContentHandler extends ContentHandler {
    static final ContentHandler INSTANCE = new UnknownContentHandler();
    
    // 返回目标资源的内容，返回的形式取决于资源的类型(不一定总是输入流)
    public Object getContent(URLConnection connection) throws IOException {
        return connection.getInputStream();
    }
}
