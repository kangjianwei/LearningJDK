package jdk.internal.agent.resources;

import java.util.ListResourceBundle;

public final class agent_zh_CN extends ListResourceBundle {
    protected final Object[][] getContents() {
        return new Object[][] {
            { "agent.err.access.file.not.readable", "\u8BBF\u95EE\u6587\u4EF6\u4E0D\u53EF\u8BFB\u53D6" },
            { "agent.err.access.file.notfound", "\u627E\u4E0D\u5230\u8BBF\u95EE\u6587\u4EF6" },
            { "agent.err.access.file.notset", "\u672A\u6307\u5B9A\u8BBF\u95EE\u6587\u4EF6, \u4F46 com.sun.management.jmxremote.authenticate=true" },
            { "agent.err.access.file.read.failed", "\u8BFB\u53D6\u8BBF\u95EE\u6587\u4EF6\u5931\u8D25" },
            { "agent.err.agentclass.access.denied", "\u62D2\u7EDD\u8BBF\u95EE premain(String)" },
            { "agent.err.agentclass.failed", "\u7BA1\u7406\u4EE3\u7406\u7C7B\u5931\u8D25 " },
            { "agent.err.agentclass.notfound", "\u627E\u4E0D\u5230\u7BA1\u7406\u4EE3\u7406\u7C7B" },
            { "agent.err.configfile.access.denied", "\u62D2\u7EDD\u8BBF\u95EE\u914D\u7F6E\u6587\u4EF6" },
            { "agent.err.configfile.closed.failed", "\u672A\u80FD\u5173\u95ED\u914D\u7F6E\u6587\u4EF6" },
            { "agent.err.configfile.failed", "\u672A\u80FD\u8BFB\u53D6\u914D\u7F6E\u6587\u4EF6" },
            { "agent.err.configfile.notfound", "\u627E\u4E0D\u5230\u914D\u7F6E\u6587\u4EF6" },
            { "agent.err.connector.server.io.error", "JMX \u8FDE\u63A5\u5668\u670D\u52A1\u5668\u901A\u4FE1\u9519\u8BEF" },
            { "agent.err.error", "\u9519\u8BEF" },
            { "agent.err.exception", "\u4EE3\u7406\u629B\u51FA\u5F02\u5E38\u9519\u8BEF" },
            { "agent.err.exportaddress.failed", "\u672A\u80FD\u5C06 JMX \u8FDE\u63A5\u5668\u5730\u5740\u5BFC\u51FA\u5230\u68C0\u6D4B\u7F13\u51B2\u533A" },
            { "agent.err.file.access.not.restricted", "\u5FC5\u987B\u9650\u5236\u6587\u4EF6\u8BFB\u53D6\u8BBF\u95EE\u6743\u9650" },
            { "agent.err.file.not.found", "\u627E\u4E0D\u5230\u6587\u4EF6" },
            { "agent.err.file.not.readable", "\u6587\u4EF6\u4E0D\u53EF\u8BFB\u53D6" },
            { "agent.err.file.not.set", "\u672A\u6307\u5B9A\u6587\u4EF6" },
            { "agent.err.file.read.failed", "\u672A\u80FD\u8BFB\u53D6\u6587\u4EF6" },
            { "agent.err.invalid.agentclass", "com.sun.management.agent.class \u5C5E\u6027\u503C\u65E0\u6548" },
            { "agent.err.invalid.jmxremote.port", "com.sun.management.jmxremote.port \u7F16\u53F7\u65E0\u6548" },
            { "agent.err.invalid.jmxremote.rmi.port", "com.sun.management.jmxremote.rmi.port \u7F16\u53F7\u65E0\u6548" },
            { "agent.err.invalid.option", "\u6307\u5B9A\u7684\u9009\u9879\u65E0\u6548" },
            { "agent.err.invalid.state", "\u65E0\u6548\u7684\u4EE3\u7406\u72B6\u6001: {0}" },
            { "agent.err.password.file.access.notrestricted", "\u5FC5\u987B\u9650\u5236\u53E3\u4EE4\u6587\u4EF6\u8BFB\u53D6\u8BBF\u95EE\u6743\u9650" },
            { "agent.err.password.file.not.readable", "\u53E3\u4EE4\u6587\u4EF6\u4E0D\u53EF\u8BFB\u53D6" },
            { "agent.err.password.file.notfound", "\u627E\u4E0D\u5230\u53E3\u4EE4\u6587\u4EF6" },
            { "agent.err.password.file.notset", "\u672A\u6307\u5B9A\u53E3\u4EE4\u6587\u4EF6, \u4F46 com.sun.management.jmxremote.authenticate=true" },
            { "agent.err.password.file.read.failed", "\u8BFB\u53D6\u53E3\u4EE4\u6587\u4EF6\u5931\u8D25" },
            { "agent.err.premain.notfound", "\u4EE3\u7406\u7C7B\u4E2D\u4E0D\u5B58\u5728 premain(String)" },
            { "agent.err.warning", "\u8B66\u544A" },
            { "jmxremote.ConnectorBootstrap.file.readonly", "\u5FC5\u987B\u9650\u5236\u6587\u4EF6\u8BFB\u53D6\u8BBF\u95EE\u6743\u9650: {0}" },
            { "jmxremote.ConnectorBootstrap.noAuthentication", "\u65E0\u9A8C\u8BC1" },
            { "jmxremote.ConnectorBootstrap.password.readonly", "\u5FC5\u987B\u9650\u5236\u53E3\u4EE4\u6587\u4EF6\u8BFB\u53D6\u8BBF\u95EE\u6743\u9650: {0}" },
            { "jmxremote.ConnectorBootstrap.ready", "\u4F4D\u4E8E{0}\u7684 JMX \u8FDE\u63A5\u5668\u5DF2\u5C31\u7EEA" },
            { "jmxremote.ConnectorBootstrap.starting", "\u6B63\u5728\u542F\u52A8 JMX \u8FDE\u63A5\u5668\u670D\u52A1\u5668: " },
        };
    }
}
