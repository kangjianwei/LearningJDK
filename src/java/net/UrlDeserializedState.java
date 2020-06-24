package java.net;

final class UrlDeserializedState {
    private final String protocol;
    private final String host;
    private final int port;
    private final String authority;
    private final String file;
    private final String ref;
    private final int hashCode;
    
    public UrlDeserializedState(String protocol, String host, int port, String authority, String file, String ref, int hashCode) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.authority = authority;
        this.file = file;
        this.ref = ref;
        this.hashCode = hashCode;
    }
    
    String getProtocol() {
        return protocol;
    }
    
    String getHost() {
        return host;
    }
    
    String getAuthority() {
        return authority;
    }
    
    int getPort() {
        return port;
    }
    
    String getFile() {
        return file;
    }
    
    String getRef() {
        return ref;
    }
    
    int getHashCode() {
        return hashCode;
    }
    
    String reconstituteUrlString() {
        
        // pre-compute length of StringBuffer
        int len = protocol.length() + 1;
        
        if(authority != null && authority.length()>0) {
            len += 2 + authority.length();
        }
        
        if(file != null) {
            len += file.length();
        }
        
        if(ref != null) {
            len += 1 + ref.length();
        }
        
        StringBuilder result = new StringBuilder(len);
        
        result.append(protocol);
        result.append(":");
        
        if(authority != null && authority.length()>0) {
            result.append("//");
            result.append(authority);
        }
        
        if(file != null) {
            result.append(file);
        }
        
        if(ref != null) {
            result.append("#");
            result.append(ref);
        }
        
        return result.toString();
    }
}
