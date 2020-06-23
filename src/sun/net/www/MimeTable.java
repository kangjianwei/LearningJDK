/*
 * Copyright (c) 1994, 2011, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.www;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import jdk.internal.util.StaticProperty;

// MIME表
public class MimeTable implements FileNameMap {
    
    private static final String filePreamble = "sun.net.www MIME content-types table";
    private static final String fileMagic = "#" + filePreamble;
    
    // Will be reset if in the platform-specific data file
    private static String tempFileTemplate; // 临时文件路径
    
    /** Keyed by content type, returns MimeEntries */
    // MIME信息映射；key是MIME类型，value是MIME实体
    private Hashtable<String, MimeEntry> entries = new Hashtable<String, MimeEntry>();
    
    /** Keyed by file extension (with the .), returns MimeEntries */
    // MIME扩展名到MIME实体的映射
    private Hashtable<String, MimeEntry> extensionMap = new Hashtable<String, MimeEntry>();
    
    /**
     * For backward compatibility -- mailcap format files
     * This is not currently used, but may in the future when we add ability
     * to read BOTH the properties format and the mailcap format.
     */
    protected static String[] mailcapLocations;
    
    static {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                tempFileTemplate = System.getProperty("content.types.temp.file.template", "/tmp/%s");
                
                mailcapLocations = new String[]{System.getProperty("user.mailcap"), StaticProperty.userHome() + "/.mailcap", "/etc/mailcap", "/usr/etc/mailcap", "/usr/local/etc/mailcap",};
                
                return null;
            }
        });
    }
    
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    MimeTable() {
        load();
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Get the single instance of this class.
     * First use will load the table from a data file.
     */
    // 返回当前类的一个单例对象
    public static MimeTable getDefaultTable() {
        return DefaultInstanceHolder.defaultInstance;
    }
    
    // 返回一个MIME信息表
    public static FileNameMap loadTable() {
        return (FileNameMap) getDefaultTable();
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 添加 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 向当前MIME表添加一条MIME实体信息
    public synchronized void add(MimeEntry mimeEntry) {
        entries.put(mimeEntry.getType(), mimeEntry);
        
        String[] exts = mimeEntry.getExtensions();
        if(exts == null) {
            return;
        }
        
        for(String ext : exts) {
            extensionMap.put(ext, mimeEntry);
        }
    }
    
    /*▲ 添加 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 移除 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 从当前MIME表移除一条MIME实体信息
    public synchronized MimeEntry remove(MimeEntry entry) {
        String[] extensionKeys = entry.getExtensions();
        if(extensionKeys != null) {
            for(String extensionKey : extensionKeys) {
                extensionMap.remove(extensionKey);
            }
        }
        
        return entries.remove(entry.getType());
    }
    
    // 从当前MIME表移除指定类型的MIME实体
    public synchronized MimeEntry remove(String type) {
        MimeEntry entry = entries.get(type);
        return remove(entry);
    }
    
    /*▲ 移除 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 查找 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 根据指定的文件名，(通过其后缀)判断其MIME类型
    public synchronized String getContentTypeFor(String fileName) {
        // 根据指定的文件名，(通过其后缀)查找其匹配的MIME实体
        MimeEntry entry = findByFileName(fileName);
        
        // 获取MIME类型
        return entry != null ? entry.getType() : null;
    }
    
    
    /**
     * Locate a MimeEntry by the file extension that has been associated with it.
     * Parses general file names, and URLs.
     */
    // 根据指定的文件名，(通过其后缀)查找其匹配的MIME实体
    public MimeEntry findByFileName(String fileName) {
        String ext = "";
        
        int i = fileName.lastIndexOf('#');
        
        if(i>0) {
            // 截取"#"之前的部分(这里可能是有BUG，应当是substring(0, i))
            fileName = fileName.substring(0, i - 1);
        }
        
        i = fileName.lastIndexOf('.');
        
        // REMIND: OS specific delimters appear here
        i = Math.max(i, fileName.lastIndexOf('/'));
        i = Math.max(i, fileName.lastIndexOf('?'));
        
        if(i != -1 && fileName.charAt(i) == '.') {
            ext = fileName.substring(i).toLowerCase();
        }
        
        // 根据指定的扩展名，查找其对应的MIME实体
        return findByExt(ext);
    }
    
    // 根据指定的MIME描述或MIME类型查找对应的MIME实体
    public synchronized MimeEntry findByDescription(String descriptionOrType) {
        // 返回当前MIME表中所有MIME实体
        Enumeration<MimeEntry> elems = elements();
        
        while(elems.hasMoreElements()) {
            MimeEntry entry = elems.nextElement();
            if(descriptionOrType.equals(entry.getDescription())) {
                return entry;
            }
        }
        
        // 在当前MIME表中查询指定类型的MIME实体
        return find(descriptionOrType);   // We failed, now try treating description as type
    }
    
    /**
     * Locate a MimeEntry by the file extension that has been associated
     * with it.
     */
    // 根据指定的扩展名，查找其对应的MIME实体
    public synchronized MimeEntry findByExt(String fileExtension) {
        return extensionMap.get(fileExtension);
    }
    
    // 在当前MIME表中查询指定类型的MIME实体
    public synchronized MimeEntry find(String type) {
        MimeEntry entry = entries.get(type);
        if(entry != null) {
            return entry;
        }
        
        // 尝试查找通配实体
        Enumeration<MimeEntry> elems = entries.elements();
        while(elems.hasMoreElements()) {
            MimeEntry wild = elems.nextElement();
            
            // 如果wild以是"xxx/*"这类通配实体，type也以"xxx/*"开头，则可以返回wild
            if(wild.matches(type)) {
                return wild;
            }
        }
        
        return entry;
    }
    
    /*▲ 查找 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 统计 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回当前MIME表中的实体数量
    public synchronized int getSize() {
        return entries.size();
    }
    
    /*▲ 统计 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 获取当前MIME表中的信息
    public Properties getAsProperties() {
        Properties properties = new Properties();
        
        Enumeration<MimeEntry> elems = elements();
        while(elems.hasMoreElements()) {
            MimeEntry entry = elems.nextElement();
            properties.put(entry.getType(), entry.toProperty());
        }
        
        return properties;
    }
    
    // 将当前MIME表中的信息写入指定的文件
    protected boolean saveAsProperties(File file) {
        FileOutputStream os = null;
        
        try {
            os = new FileOutputStream(file);
            
            // 获取当前MIME表中的信息
            Properties properties = getAsProperties();
            properties.put("temp.file.template", tempFileTemplate);
            
            // Perform the property security check for user.name
            SecurityManager sm = System.getSecurityManager();
            if(sm != null) {
                sm.checkPropertyAccess("user.name");
            }
            
            String tag;
            
            // 获取用户主机名称，如kang
            String user = StaticProperty.userName();
            
            if(user != null) {
                tag = "; customized for " + user;
                properties.store(os, filePreamble + tag);
            } else {
                properties.store(os, filePreamble);
            }
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if(os != null) {
                try {
                    os.close();
                } catch(IOException e) {
                
                }
            }
        }
        
        return true;
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 返回临时文件路径
    String getTempFileTemplate() {
        return tempFileTemplate;
    }
    
    // 返回当前MIME表中所有MIME实体
    public synchronized Enumeration<MimeEntry> elements() {
        return entries.elements();
    }
    
    // 从本地加载并解析预设的MIME信息表
    public synchronized void load() {
        Properties entries = new Properties();
        File file = null;
        InputStream in;
        
        // First try to load the user-specific table, if it exists
        String userTablePath = System.getProperty("content.types.user.table");
        
        // 尝试从指定的位置加载MIME信息
        if(userTablePath != null && (file = new File(userTablePath)).exists()) {
            try {
                in = new FileInputStream(file);
            } catch(FileNotFoundException e) {
                System.err.println("Warning: " + file.getPath() + " mime table not found.");
                return;
            }
        } else {
            // 参见：sun/net/www/content-types.propertie文件
            in = MimeTable.class.getResourceAsStream("content-types.properties");
            if(in == null) {
                throw new InternalError("default mime table not found");
            }
        }
        
        try(BufferedInputStream bin = new BufferedInputStream(in)) {
            // 解析MIME信息
            entries.load(bin);
        } catch(IOException e) {
            System.err.println("Warning: " + e.getMessage());
        }
        
        parse(entries);
    }
    
    // 解析MIME信息属性集
    void parse(Properties entries) {
        // first, strip out the platform-specific temp file template
        String tempFileTemplate = (String) entries.get("temp.file.template");
        if(tempFileTemplate != null) {
            entries.remove("temp.file.template");
            // 记录临时文件路径
            MimeTable.tempFileTemplate = tempFileTemplate;
        }
        
        // now, parse the mime-type spec's
        Enumeration<?> types = entries.propertyNames();
        while (types.hasMoreElements()) {
            String type = (String)types.nextElement();
            String attrs = entries.getProperty(type);
            parse(type, attrs);
        }
    }
    
    // Table format:
    //
    // <entry> ::= <table_tag> | <type_entry>
    //
    // <table_tag> ::= <table_format_version> | <temp_file_template>
    //
    // <type_entry> ::= <type_subtype_pair> '=' <type_attrs_list>
    //
    // <type_subtype_pair> ::= <type> '/' <subtype>
    //
    // <type_attrs_list> ::= <attr_value_pair> [ ';' <attr_value_pair> ]* | [ <attr_value_pair> ]+
    //
    // <attr_value_pair> ::= <attr_name> '=' <attr_value>
    //
    // <attr_name> ::= 'description' | 'action' | 'application' | 'file_extensions' | 'icon'
    //
    // <attr_value> ::= <legal_char>*
    //
    // Embedded ';' in an <attr_value> are quoted with leading '\' .
    //
    // Interpretation of <attr_value> depends on the <attr_name> it is associated with.
    
    /*
     * 解析指定的类型和属性，格式如：
     *  application/zip: \
     * 	       description=Zip File;\
     * 	       file_extensions=.zip;\
     * 	       icon=zip;\
     * 	       action=save
     */
    void parse(String type, String attrs) {
        MimeEntry newEntry = new MimeEntry(type);
        
        // REMIND handle embedded ';' and '|' and literal '"'
        StringTokenizer tokenizer = new StringTokenizer(attrs, ";");
        
        while(tokenizer.hasMoreTokens()) {
            // 每个pair例如"file_extensions=.zip"
            String pair = tokenizer.nextToken();
            parse(pair, newEntry);
        }
        
        add(newEntry);
    }
    
    // 解析每一对属性
    void parse(String pair, MimeEntry entry) {
        // REMIND add exception handling...
        String name = null;     // 例如"file_extensions"
        String value = null;    // 例如".zip"
        
        boolean gotName = false;
        
        StringTokenizer tokenizer = new StringTokenizer(pair, "=");
        
        while(tokenizer.hasMoreTokens()) {
            if(gotName) {
                value = tokenizer.nextToken().trim();
            } else {
                name = tokenizer.nextToken().trim();
                gotName = true;
            }
        }
        
        fill(entry, name, value);
    }
    
    // 向指定类型的实体填充对应的属性
    void fill(MimeEntry entry, String name, String value) {
        if("description".equalsIgnoreCase(name)) {
            entry.setDescription(value);
        } else if("action".equalsIgnoreCase(name)) {
            entry.setAction(getActionCode(value));
        } else if("application".equalsIgnoreCase(name)) {
            entry.setCommand(value);
        } else if("icon".equalsIgnoreCase(name)) {
            entry.setImageFileName(value);
        } else if("file_extensions".equalsIgnoreCase(name)) {
            entry.setExtensions(value);
        }
        
        // else illegal name exception
    }
    
    // 解析扩展名信息
    String[] getExtensions(String list) {
        StringTokenizer tokenizer = new StringTokenizer(list, ",");
        
        int n = tokenizer.countTokens();
        
        String[] extensions = new String[n];
        
        for(int i = 0; i<n; i++) {
            extensions[i] = tokenizer.nextToken();
        }
        
        return extensions;
    }
    
    // 解析action信息
    int getActionCode(String action) {
        for(int i = 0; i<MimeEntry.actionKeywords.length; i++) {
            if(action.equalsIgnoreCase(MimeEntry.actionKeywords[i])) {
                return i;
            }
        }
        
        return MimeEntry.UNKNOWN;
    }
    
    
    private static class DefaultInstanceHolder {
        static final MimeTable defaultInstance = getDefaultInstance();
        
        static MimeTable getDefaultInstance() {
            return AccessController.doPrivileged(new PrivilegedAction<MimeTable>() {
                public MimeTable run() {
                    MimeTable instance = new MimeTable();
                    URLConnection.setFileNameMap(instance);
                    return instance;
                }
            });
        }
    }
    
}
