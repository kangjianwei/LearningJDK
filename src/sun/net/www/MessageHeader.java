/*
 * Copyright (c) 1995, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*-
 *      news stream opener
 */

package sun.net.www;

import java.io.*;
import java.util.Collections;
import java.util.*;

/**
 * An RFC 844 or MIME message header.
 * Includes methods for parsing headers from incoming streams, fetching values, setting values, and printing headers.
 *
 * Key values of null are legal:
 * they indicate lines in the header that don't have a valid key, but do have
 * a value (this isn't legal according to the standard, but lines like this are everywhere).
 */
// 消息头，是一个键值对容器，通常可用来存储MIME信息头、请求头、响应头等
public class MessageHeader {
    private String[] keys;
    private String[] values;
    private int nkeys;
    
    public MessageHeader() {
        // 分配存储
        grow();
    }
    
    public MessageHeader(InputStream is) throws java.io.IOException {
        // 从指定的输入流中解析消息头
        parseHeader(is);
    }
    
    /**
     * Convert a message-id string to canonical form (strips off leading and trailing {@literal <>s})
     */
    // 转换消息ID为规范形式(移除首尾的<>和空白)
    public static String canonicalID(String id) {
        if(id == null) {
            return "";
        }
        
        int st = 0;
        int len = id.length();
        boolean substr = false;
        int c;
        
        while(st<len && ((c = id.charAt(st)) == '<' || c<=' ')) {
            st++;
            substr = true;
        }
        
        while(st<len && ((c = id.charAt(len - 1)) == '>' || c<=' ')) {
            len--;
            substr = true;
        }
        
        return substr ? id.substring(st, len) : id;
    }
    
    /**
     * Returns list of header names in a comma separated list
     */
    // 返回用","分割的消息头的key
    public synchronized String getHeaderNamesInList() {
        StringJoiner joiner = new StringJoiner(",");
        
        for(int i = 0; i<nkeys; i++) {
            joiner.add(keys[i]);
        }
        
        return joiner.toString();
    }
    
    /**
     * Reset a message header (all key/values removed)
     */
    // 重置当前消息头
    public synchronized void reset() {
        keys = null;
        values = null;
        nkeys = 0;
        grow();
    }
    
    /**
     * Find the value that corresponds to this key.
     * It finds only the first occurrence of the key.
     *
     * @param k the key to find.
     *
     * @return null if not found.
     */
    // 返回消息头中指定key对应的value
    public synchronized String findValue(String key) {
        if(key == null) {
            for(int i = nkeys; --i >= 0; ) {
                if(keys[i] == null) {
                    return values[i];
                }
            }
        } else {
            for(int i = nkeys; --i >= 0; ) {
                if(key.equalsIgnoreCase(keys[i])) {
                    return values[i];
                }
            }
        }
        
        return null;
    }
    
    // 返回指定的key在消息头中的索引
    public synchronized int getKey(String key) {
        for(int i = nkeys; --i >= 0; ) {
            if(keys[i] == key || (key != null && key.equalsIgnoreCase(keys[i]))) {
                return i;
            }
        }
        
        return -1;
    }
    
    // 返回消息头中指定索引处的key
    public synchronized String getKey(int n) {
        if(n<0 || n >= nkeys) {
            return null;
        }
        
        return keys[n];
    }
    
    // 返回消息头中指定索引处的value
    public synchronized String getValue(int n) {
        if(n<0 || n >= nkeys) {
            return null;
        }
        
        return values[n];
    }
    
    /**
     * Deprecated: Use multiValueIterator() instead.
     *
     * Find the next value that corresponds to this key.
     * It finds the first value that follows v. To iterate
     * over all the values of a key use:
     * <pre>
     *          for(String v=h.findValue(k); v!=null; v=h.findNextValue(k, v)) {
     *              ...
     *          }
     *  </pre>
     */
    // 基于映射<key, value>查找key对应的下一个值，已过时，建议使用替代
    public synchronized String findNextValue(String key, String value) {
        boolean foundV = false;
        
        if(key == null) {
            for(int i = nkeys; --i >= 0; ) {
                if(keys[i] == null) {
                    if(foundV) {
                        return values[i];
                    }
                    
                    if(values[i] == value) {
                        foundV = true;
                    }
                }
            }
        } else {
            for(int i = nkeys; --i >= 0; ) {
                if(key.equalsIgnoreCase(keys[i])) {
                    if(foundV) {
                        return values[i];
                    }
                    
                    if(values[i] == value) {
                        foundV = true;
                    }
                }
            }
        }
        
        return null;
    }
    
    // 返回当前消息头(的只读形式)
    public synchronized Map<String, List<String>> getHeaders() {
        return getHeaders(null);
    }
    
    // 从当前消息头中移除excludeKey中的key对应的条目
    public synchronized Map<String, List<String>> getHeaders(String[] excludeKey) {
        return filterAndAddHeaders(excludeKey, null);
    }
    
    // 从当前消息头中移除excludeKey中的key对应的条目，随后向当前消息头添加include中包含的条目
    public synchronized Map<String, List<String>> filterAndAddHeaders(String[] excludeKey, Map<String, List<String>> include) {
        boolean skipIt = false;
        
        Map<String, List<String>> map = new HashMap<>();
        
        // 首先跳过该excludeKey中的key对应的条目
        for(int i = nkeys; --i >= 0; ) {
            if(excludeKey != null) {
                // check if the key is in the excludeList. if so, don't include it in the Map.
                for(String key : excludeKey) {
                    if((key != null) && (key.equalsIgnoreCase(keys[i]))) {
                        skipIt = true;
                        break;
                    }
                }
            }
            
            if(!skipIt) {
                List<String> list = map.computeIfAbsent(keys[i], k -> new ArrayList<>());
                
                list.add(values[i]);
            } else {
                // reset the flag
                skipIt = false;
            }
        }
        
        // 添加include中包含的条目
        if(include != null) {
            for(Map.Entry<String, List<String>> entry : include.entrySet()) {
                List<String> list = map.computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
                list.addAll(entry.getValue());
            }
        }
        
        map.replaceAll((k, v) -> Collections.unmodifiableList(map.get(k)));
        
        return Collections.unmodifiableMap(map);
    }
    
    /**
     * Prints the key-value pairs represented by this header.
     * Also prints the RFC required blank line at the end.
     * Omits pairs with a null key.
     */
    // 向指定的输出流打印消息头
    public synchronized void print(PrintStream printStream) {
        for(int i = 0; i<nkeys; i++) {
            if(keys[i] != null) {
                printStream.print(keys[i] + (values[i] != null ? ": " + values[i] : "") + "\r\n");
            }
        }
        
        printStream.print("\r\n");
        printStream.flush();
    }
    
    /**
     * Adds a key value pair to the end of the header.
     * Duplicates are allowed
     */
    // 向消息头的尾部添加一个映射<key, value>
    public synchronized void add(String key, String value) {
        grow();
        
        keys[nkeys] = key;
        values[nkeys] = value;
        
        nkeys++;
    }
    
    /**
     * Prepends a key value pair to the beginning of the
     * header.  Duplicates are allowed
     */
    // 向消息头的头部添加一个映射<key, value>
    public synchronized void prepend(String key, String value) {
        grow();
        
        for(int i = nkeys; i>0; i--) {
            keys[i] = keys[i - 1];
            values[i] = values[i - 1];
        }
        
        keys[0] = key;
        values[0] = value;
        
        nkeys++;
    }
    
    /**
     * Overwrite the previous key/val pair at location 'i'
     * with the new k/v.  If the index didn't exist before
     * the key/val is simply tacked onto the end.
     */
    // 为消息头的index处设置映射<key, value>，如果存在则覆盖
    public synchronized void set(int index, String key, String value) {
        grow();
        
        if(index<0) {
            return;
        }
        
        if(index >= nkeys) {
            add(key, value);
        } else {
            keys[index] = key;
            values[index] = value;
        }
    }
    
    /**
     * Remove the key from the header. If there are multiple values under
     * the same key, they are all removed.
     * Nothing is done if the key doesn't exist.
     * After a remove, the other pairs' order are not changed.
     *
     * @param k the key to remove
     */
    // 删除消息头中指定key对应的条目
    public synchronized void remove(String key) {
        if(key == null) {
            for(int i = 0; i<nkeys; i++) {
                while(keys[i] == null && i<nkeys) {
                    for(int j = i; j<nkeys - 1; j++) {
                        keys[j] = keys[j + 1];
                        values[j] = values[j + 1];
                    }
                    nkeys--;
                }
            }
        } else {
            for(int i = 0; i<nkeys; i++) {
                while(key.equalsIgnoreCase(keys[i]) && i<nkeys) {
                    for(int j = i; j<nkeys - 1; j++) {
                        keys[j] = keys[j + 1];
                        values[j] = values[j + 1];
                    }
                    nkeys--;
                }
            }
        }
    }
    
    /**
     * Sets the value of a key.
     * If the key already exists in the header, it's value will be changed.
     * Otherwise a new key/value pair will be added to the end of the header.
     */
    // 为消息头设置映射<key, value>，如果存在则覆盖
    public synchronized void set(String key, String value) {
        for(int i = nkeys; --i >= 0; ) {
            if(key.equalsIgnoreCase(keys[i])) {
                values[i] = value;
                return;
            }
        }
        
        add(key, value);
    }
    
    /**
     * Set's the value of a key only if there is no
     * key with that value already.
     */
    // 如果消息头中不存在指定key对应的条目，则存入<key, value>映射
    public synchronized void setIfNotSet(String key, String value) {
        // 查找消息头中指定key对应的值
        if(findValue(key) == null) {
            // 如果没找到该值，则存入value
            add(key, value);
        }
    }
    
    public synchronized String toString() {
        String result = super.toString() + nkeys + " pairs: ";
        
        for(int i = 0; i<keys.length && i<nkeys; i++) {
            result += "{" + keys[i] + ": " + values[i] + "}";
        }
        
        return result;
    }
    
    /**
     * Removes bare Negotiate and Kerberos headers when an "NTLM ..." appears.
     * All Performed on headers with key being key.
     *
     * @return true if there is a change
     */
    // 如果key对应的value中存在以"NTLM "开头的，则移除key对应的value为"Negotiate"或"Kerberos"的条目
    public boolean filterNTLMResponses(String key) {
        boolean found = false;
        
        // 查询key对应条目中，是否存在以"NTLM "开头的值
        for(int i = 0; i<nkeys; i++) {
            if(key.equalsIgnoreCase(keys[i]) && values[i] != null && values[i].length()>5 && values[i].substring(0, 5).equalsIgnoreCase("NTLM ")) {
                found = true;
                break;
            }
        }
        
        if(!found) {
            return false;
        }
        
        int j = 0;
        
        for(int i = 0; i<nkeys; i++) {
            // 跳过"Negotiate"和"Kerberos"值
            if(key.equalsIgnoreCase(keys[i]) && ("Negotiate".equalsIgnoreCase(values[i]) || "Kerberos".equalsIgnoreCase(values[i]))) {
                continue;
            }
            
            if(i != j) {
                keys[j] = keys[i];
                values[j] = values[i];
            }
            
            j++;
        }
        
        if(j != nkeys) {
            nkeys = j;
            return true;
        }
        
        return false;
    }
    
    /**
     * return an Iterator that returns all values of a particular key in sequence
     */
    // 返回指定key对应的value的迭代器
    public Iterator<String> multiValueIterator(String key) {
        return new HeaderIterator(key, this);
    }
    
    /** Parse a MIME header from an input stream. */
    // 从指定的输入流中解析消息头
    public void parseHeader(InputStream is) throws java.io.IOException {
        synchronized(this) {
            nkeys = 0;
        }
        
        mergeHeader(is);
    }
    
    /** Parse and merge a MIME header from an input stream. */
    // 从指定的输入流中解析消息头
    @SuppressWarnings("fallthrough")
    public void mergeHeader(InputStream is) throws java.io.IOException {
        if(is == null) {
            return;
        }
    
        char[] s = new char[10];
    
        int firstc = is.read();
    
        while(firstc != '\n' && firstc != '\r' && firstc >= 0) {
            int len = 0;
            int keyend = -1;
            int c;
            boolean inKey = firstc>' ';
        
            s[len++] = (char) firstc;

parseloop:
            {
                while((c = is.read()) >= 0) {
                    switch(c) {
                        case ':':
                            if(inKey && len>0) {
                                keyend = len;
                            }
                            inKey = false;
                            break;
                    
                        case '\t':
                            c = ' ';
                        
                            /*fall through*/
                        case ' ':
                            inKey = false;
                            break;
                    
                        case '\r':
                        case '\n':
                            firstc = is.read();
                            if(c == '\r' && firstc == '\n') {
                                firstc = is.read();
                                if(firstc == '\r') {
                                    firstc = is.read();
                                }
                            }
                        
                            if(firstc == '\n' || firstc == '\r' || firstc>' ') {
                                break parseloop;
                            }
                        
                            /* continuation */
                            c = ' ';
                            break;
                    }
                
                    if(len >= s.length) {
                        char[] ns = new char[s.length * 2];
                        System.arraycopy(s, 0, ns, 0, len);
                        s = ns;
                    }
                
                    s[len++] = (char) c;
                }
            
                firstc = -1;
            }
        
            while(len>0 && s[len - 1]<=' ') {
                len--;
            }
        
            String k;
        
            if(keyend<=0) {
                k = null;
                keyend = 0;
            } else {
                k = String.copyValueOf(s, 0, keyend);
            
                if(keyend<len && s[keyend] == ':') {
                    keyend++;
                }
            
                while(keyend<len && s[keyend]<=' ') {
                    keyend++;
                }
            }
        
            String v;
        
            if(keyend >= len) {
                v = "";
            } else {
                v = String.copyValueOf(s, keyend, len - keyend);
            }
        
            add(k, v);
        }
    }
    
    /** grow the key/value arrays as needed */
    // 分配存储
    private void grow() {
        if(keys == null || nkeys >= keys.length) {
            String[] nk = new String[nkeys + 4];
            String[] nv = new String[nkeys + 4];
            
            if(keys != null) {
                System.arraycopy(keys, 0, nk, 0, nkeys);
            }
            
            if(values != null) {
                System.arraycopy(values, 0, nv, 0, nkeys);
            }
            
            keys = nk;
            
            values = nv;
        }
    }
    
    // 遍历指定key对应的所有value
    class HeaderIterator implements Iterator<String> {
        int index = 0;
        int next = -1;
        String key;
        boolean haveNext = false;
        Object lock;
        
        public HeaderIterator(String key, Object lock) {
            this.key = key;
            this.lock = lock;
        }
        
        public boolean hasNext() {
            synchronized(lock) {
                if(haveNext) {
                    return true;
                }
                
                while(index<nkeys) {
                    if(key.equalsIgnoreCase(keys[index])) {
                        haveNext = true;
                        next = index++;
                        return true;
                    }
                    
                    index++;
                }
                
                return false;
            }
        }
        
        public String next() {
            synchronized(lock) {
                if(haveNext) {
                    haveNext = false;
                    return values[next];
                }
                
                if(hasNext()) {
                    return next();
                }
                
                throw new NoSuchElementException("No more elements");
            }
        }
        
        public void remove() {
            throw new UnsupportedOperationException("remove not allowed");
        }
    }
}
