/*
 * Copyright (c) 1994, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.InputStream;
import java.util.StringJoiner;
import java.util.StringTokenizer;

// MIME实体
public class MimeEntry implements Cloneable {
    
    private String tempFileNameTemplate;    // 临时文件路径
    
    /**
     * 以一条MIME消息为例：
     *
     * application/zip: \
     * description=Zip File; \
     * file_extensions=.zip; \
     * icon=zip; \
     * action=save
     */
    private String typeName;        // 形如application/zip
    private String description;     // 形如Zip File
    private String fileExtensions[];// 形如.zip
    private String imageFileName;   // 形如zip(会被包装为java.net.ftp.imagepath.zip.gif这类形式)
    private int action;          // 形如save(的代号)
    private String command;         // 需要对该实体执行的命令
    
    boolean starred;                // 当前MIME类型是否为通配符(以"/*"结尾)
    
    // Actions
    public static final int UNKNOWN = 0;
    public static final int LOAD_INTO_BROWSER = 1;
    public static final int SAVE_TO_FILE = 2;
    public static final int LAUNCH_APPLICATION = 3;
    
    // 对应上述action的关键字
    static final String[] actionKeywords = {"unknown", "browser", "save", "application",};
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Construct an empty entry of the given type and subtype.
     */
    public MimeEntry(String type) {
        // Default action is UNKNOWN so clients can decide what the default should be, typically save to file or ask user.
        this(type, UNKNOWN, null, null, null);
    }
    
    /**
     * The next two constructors are used only by the deprecated PlatformMimeTable classes or,
     * in last case, is called by the public constructor.
     * They are kept here anticipating putting support for mailcap formatted config files back in
     * (so BOTH the properties format and the mailcap formats are supported).
     */
    MimeEntry(String type, String imageFileName, String extensionString) {
        this.typeName = type.toLowerCase();
        this.action = UNKNOWN;
        this.command = null;
        this.imageFileName = imageFileName;
        setExtensions(extensionString);
        this.starred = isStarred(typeName);
    }
    
    // For use with MimeTable::parseMailCap
    MimeEntry(String typeName, int action, String command, String tempFileNameTemplate) {
        this.typeName = typeName.toLowerCase();
        this.action = action;
        this.command = command;
        this.imageFileName = null;
        this.fileExtensions = null;
        this.tempFileNameTemplate = tempFileNameTemplate;
    }
    
    // This is the one called by the public constructor.
    MimeEntry(String typeName, int action, String command, String imageFileName, String fileExtensions[]) {
        this.typeName = typeName.toLowerCase();
        this.action = action;
        this.command = command;
        this.imageFileName = imageFileName;
        this.fileExtensions = fileExtensions;
        this.starred = isStarred(typeName);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ get ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回MIME类型
    public synchronized String getType() {
        return typeName;
    }
    
    // 返回MIME描述
    public synchronized String getDescription() {
        return (description != null ? description : typeName);
    }
    
    // 返回MIME扩展名(以字符串数组形式返回)
    public synchronized String[] getExtensions() {
        return fileExtensions;
    }
    
    // 返回MIME扩展名，多个扩展名之间用","隔开
    public synchronized String getExtensionsAsList() {
        String extensionsAsString = "";
        
        if(fileExtensions != null) {
            for(int i = 0; i<fileExtensions.length; i++) {
                extensionsAsString += fileExtensions[i];
                if(i<(fileExtensions.length - 1)) {
                    extensionsAsString += ",";
                }
            }
        }
        
        return extensionsAsString;
    }
    
    /**
     * ??? what to return for the image
     * -- the file name or should this return something more advanced like an image source or something?
     * returning the name has the least policy associated with it.
     * pro tempore, we'll use the name.
     */
    // 返回图标名称
    public String getImageFileName() {
        return imageFileName;
    }
    
    // 返回待执行动作
    public synchronized int getAction() {
        return action;
    }
    
    // 返回命令行
    public synchronized String getLaunchString() {
        return command;
    }
    
    
    // 返回临时文件路径
    public String getTempFileTemplate() {
        return tempFileNameTemplate;
    }
    
    /*▲ get ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ set ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 设置MIME类型
    public synchronized void setType(String type) {
        typeName = type.toLowerCase();
    }
    
    // 设置MIME描述
    public synchronized void setDescription(String description) {
        this.description = description;
    }
    
    // 设置MIME扩展名，多个扩展名之间用","隔开
    public synchronized void setExtensions(String extensionString) {
        StringTokenizer extTokens = new StringTokenizer(extensionString, ",");
        
        int numExts = extTokens.countTokens();
        
        String extensionStrings[] = new String[numExts];
        for(int i = 0; i<numExts; i++) {
            String ext = (String) extTokens.nextElement();
            extensionStrings[i] = ext.trim();
        }
        
        fileExtensions = extensionStrings;
    }
    
    // 设置图标名称
    public synchronized void setImageFileName(String filename) {
        File file = new File(filename);
        if(file.getParent() == null) {
            imageFileName = System.getProperty("java.net.ftp.imagepath." + filename);
        } else {
            imageFileName = filename;
        }
        
        // 如果不存在'.'，则添加后缀.gif
        if(filename.lastIndexOf('.')<0) {
            imageFileName = imageFileName + ".gif";
        }
    }
    
    // 设置待执行动作
    public synchronized void setAction(int action) {
        this.action = action;
    }
    
    // 设置待执行动作和命令行
    public synchronized void setAction(int action, String command) {
        this.action = action;
        this.command = command;
    }
    
    // 设置命令行
    public synchronized void setCommand(String command) {
        this.command = command;
    }
    
    /*▲ set ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 执行 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Invoke the MIME type specific behavior for this MIME type.
     * Returned value can be one of several types:
     * <ol>
     * <li>A thread -- the caller can choose when to launch this thread.
     * <li>A string -- the string is loaded into the browser directly.
     * <li>An input stream -- the caller can read from this byte stream and
     *     will typically store the results in a file.
     * <li>A document (?) --
     * </ol>
     */
    /*
     * 根据当前MIME实体的action做出响应，对指定连接处的资源进行特定的操作：
     * action="save"       : 返回待保存资源的输入流
     * action="browser"    : 返回目标资源的内容(可能以字节流形式返回，也可能以字节数组形式返回等)
     * action="application": 需要将输入流中的资源内容存储到临时目录，然后对该临时文件执行预设的命令行，比如打开操作
     * action="unknown"    : 返回null
     */
    public Object launch(java.net.URLConnection connection, InputStream is, MimeTable mimeTable) throws ApplicationLaunchException {
        // 判断MIME动作
        switch(action) {
            // "save"
            case SAVE_TO_FILE:
                // REMIND: is this really the right thing to do?
                try {
                    return is;
                } catch(Exception e) {
                    // I18N
                    return "Load to file failed:\n" + e;
                }
                
                // "browser"
            case LOAD_INTO_BROWSER:
                /*
                 * REMIND: invoke the content handler?
                 * may be the right thing to do, may not be
                 * -- short term where docs are not loaded asynch,
                 * loading and returning the content is the right thing to do.
                 */
                try {
                    return connection.getContent();
                } catch(Exception e) {
                    return null;
                }
                
                // "application"
            case LAUNCH_APPLICATION: {
                String threadName = command;
                int fst = threadName.indexOf(' ');
                if(fst>0) {
                    threadName = threadName.substring(0, fst);
                }
                
                // 备用临时文件路径
                String tempFile = mimeTable.getTempFileTemplate();
                
                // 构造执行约束命令的线程
                return new MimeLauncher(this, connection, is, tempFile, threadName);
            }
            
            // "unknown"
            case UNKNOWN:
                // REMIND: What to do here?
                return null;
        }
        
        return null;
    }
    
    /*▲ 执行 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 判断给定的MIME类型是否与当前MIME实体的类型匹配
    public boolean matches(String type) {
        // 如果当前MIME类型为通配符(以"/*"结尾)
        if(starred) {
            /* REMIND: is this the right thing or not? */
            // 要求type以当前MIME类型的字符串开头
            return type.startsWith(typeName);
        }
        
        return type.equals(typeName);
    }
    
    // 合成MIME消息为一个字符串
    public synchronized String toProperty() {
        StringJoiner sj = new StringJoiner("; ");
        
        int action = getAction();
        if(action != MimeEntry.UNKNOWN) {
            sj.add("action=" + actionKeywords[action]);
        }
        
        String command = getLaunchString();
        if(command != null && command.length()>0) {
            sj.add("application=" + command);
        }
        
        String image = getImageFileName();
        if (image != null) {
            sj.add("icon=" + image);
        }
        
        String extensions = getExtensionsAsList();
        if (extensions.length() > 0) {
            sj.add("file_extensions=" + extensions);
        }
        
        String description = getDescription();
        if (description != null && !description.equals(getType())) {
            sj.add("description=" + description);
        }
        
        return sj.toString();
    }
    
    
    public String toString() {
        return "MimeEntry[contentType=" + typeName + ", image=" + imageFileName + ", action=" + action + ", command=" + command + ", extensions=" + getExtensionsAsList() + "]";
    }
    
    public Object clone() {
        // return a shallow copy of this.
        MimeEntry theClone = new MimeEntry(typeName);
        theClone.action = action;
        theClone.command = command;
        theClone.description = description;
        theClone.imageFileName = imageFileName;
        theClone.tempFileNameTemplate = tempFileNameTemplate;
        theClone.fileExtensions = fileExtensions;
        
        return theClone;
    }
    
    
    // 判断指定的MIME类型是否为通配符(以"/*"结尾)
    private boolean isStarred(String typeName) {
        return (typeName != null) && (typeName.length()>0) && (typeName.endsWith("/*"));
    }
    
}
