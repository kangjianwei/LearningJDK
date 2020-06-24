/*
 * Copyright (c) 1994, 1998, Oracle and/or its affiliates. All rights reserved.
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

import java.net.URL;
import java.io.*;
import java.util.StringTokenizer;
import sun.security.action.GetPropertyAction;

// 对指定输入源中的数据执行预设的命令
class MimeLauncher extends Thread {
    java.net.URLConnection connection;  // 目标资源
    MimeEntry mimeEntry;                // 目标资源对应的MIME实体
    InputStream is;                     // 输入源，会从中读取数据并写入到临时文件
    String execPath;                    // 执行命令行的可执行文件路径
    String genericTempFileTemplate;     // 备用临时文件路径
    
    MimeLauncher(MimeEntry mimeEntry, java.net.URLConnection connection, InputStream is, String tempFileTemplate, String threadName) throws ApplicationLaunchException {
        super(null, null, threadName, 0, false);
        this.mimeEntry = mimeEntry;
        this.connection = connection;
        this.is = is;
        this.genericTempFileTemplate = tempFileTemplate;
        
        /* get the application to launch */
        // 获取命令行
        String launchString = mimeEntry.getLaunchString();
        
        /* get a valid path to launch application - sets the execPath instance variable with the correct path. */
        // 从指定的命令行中获取可执行文件的路径，返回值指示是否获取成功
        if(findExecutablePath(launchString)) {
            return;
        }
        
        /* strip off parameters i.e %s */
        String appName;
        
        int index = launchString.indexOf(' ');
        if(index != -1) {
            appName = launchString.substring(0, index);
        } else {
            appName = launchString;
        }
        
        throw new ApplicationLaunchException(appName);
    }
    
    // 执行命令
    public void run() {
        try {
            // 获取临时文件路径
            String ofn = mimeEntry.getTempFileTemplate();
            if(ofn == null) {
                ofn = genericTempFileTemplate;
            }
            
            // 生成临时文件名称
            ofn = getTempFileName(connection.getURL(), ofn);
            
            try {
                OutputStream os = new FileOutputStream(ofn);
                byte[] buf = new byte[2048];
                int len = 0;
                try {
                    // 从is读取输入，并将其写入到临时文件
                    while((len = is.read(buf)) >= 0) {
                        os.write(buf, 0, len);
                    }
                } catch(IOException e) {
                    //System.err.println("Exception in write loop " + i);
                    //e.printStackTrace();
                } finally {
                    os.close();
                    is.close();
                }
            } catch(IOException e) {
                //System.err.println("Exception in input or output stream");
                //e.printStackTrace();
            }
            
            int inx = 0;
            
            // 获取执行命令行的可执行文件路径
            String c = execPath;
            
            // 如果可执行文件路径中包含"%t"，将其替换为"content-type"的值
            while((inx = c.indexOf("%t")) >= 0) {
                c = c.substring(0, inx) + connection.getContentType() + c.substring(inx + 2);
            }
            
            boolean substituted = false;
            
            // 如果可执行文件路径中包含"%s"，将其替换为临时文件名称
            while((inx = c.indexOf("%s")) >= 0) {
                c = c.substring(0, inx) + ofn + c.substring(inx + 2);
                substituted = true;
            }
            
            if(!substituted) {
                // 临时文件作为命令的输入
                c = c + " <" + ofn;
            }
            
            // 构造执行指定命令的进程
            Runtime.getRuntime().exec(c);
        } catch(IOException e) {
        }
    }
    
    // 生成临时文件名称
    protected String getTempFileName(URL url, String template) {
        String tempFilename = template;
        
        /*
         * Replace all but last occurrance of "%s" with timestamp to insure uniqueness.
         *
         * There's a subtle behavior here:
         * if there is anything _after_ the last "%s" we need to append it so that unusual launch strings
         * that have the datafile in the middle can still be used.
         */
        int wildcard = tempFilename.lastIndexOf("%s");
        String prefix = tempFilename.substring(0, wildcard);
        
        String suffix = "";
        if(wildcard<tempFilename.length() - 2) {
            suffix = tempFilename.substring(wildcard + 2);
        }
        
        long timestamp = System.currentTimeMillis() / 1000;
        int argIndex = 0;
        while((argIndex = prefix.indexOf("%s")) >= 0) {
            prefix = prefix.substring(0, argIndex) + timestamp + prefix.substring(argIndex + 2);
        }
        
        // Add a file name and file-extension if known
        String filename = url.getFile();
        
        String extension = "";
        int dot = filename.lastIndexOf('.');
        
        /*
         * BugId 4084826:  Temp MIME file names not always valid.
         * Fix:  don't allow slashes in the file name or extension.
         */
        if(dot >= 0 && dot>filename.lastIndexOf('/')) {
            extension = filename.substring(dot);
        }
        
        filename = "HJ" + url.hashCode();
        
        tempFilename = prefix + filename + timestamp + extension + suffix;
        
        return tempFilename;
    }
    
    /**
     * This method determines the path for the launcher application and sets the execPath instance variable.
     * It uses the exec.path property to obtain a list of paths that is in turn used to location the application.
     * If a valid path is not found, it returns false else true.
     */
    // 从指定的命令行中获取可执行文件的路径，返回值指示是否获取成功
    private boolean findExecutablePath(String str) {
        if(str == null || str.length() == 0) {
            return false;
        }
        
        String command;
        
        int index = str.indexOf(' ');
        if(index != -1) {
            // 获取首个空格之前的部分
            command = str.substring(0, index);
        } else {
            command = str;
        }
        
        // 获取执行命令行的可执行文件
        File file = new File(command);
        if(file.isFile()) {
            // Already executable as it is
            execPath = str;
            return true;
        }
        
        // 工作目录列表
        String execPathList = GetPropertyAction.privilegedGetProperty("exec.path");
        if(execPathList == null) {
            // exec.path property not set
            return false;
        }
        
        StringTokenizer iter = new StringTokenizer(execPathList, "|");
        
        while(iter.hasMoreElements()) {
            String prefix = (String) iter.nextElement();
            
            // 构造可执行文件的完整路径
            String fullCmd = prefix + File.separator + command;
            
            file = new File(fullCmd);
            if(file.isFile()) {
                execPath = prefix + File.separator + str;
                return true;
            }
        }
        
        return false; // application not found in exec.path
    }
    
}
