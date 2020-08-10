/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.util;

// IP地址转换工具
public class IPAddressUtil {
    
    private static final int INADDR4SZ = 4;     // IP4地址字节数
    private static final int INADDR16SZ = 16;   // IP6地址字节数
    private static final int INT16SZ = 2;
    
    /**
     * Converts IPv4 address in its textual presentation form
     * into its numeric binary form.
     *
     * @param src a String representing an IPv4 address in standard format
     *
     * @return a byte array representing the IPv4 numeric address
     */
    // 将文本形式的IP4地址转换为二进制形式
    @SuppressWarnings("fallthrough")
    public static byte[] textToNumericFormatV4(String ip4) {
        byte[] res = new byte[INADDR4SZ];
        
        long tmpValue = 0;
        int currByte = 0;
        boolean newOctet = true;
        
        int len = ip4.length();
        if(len == 0 || len>15) {
            return null;
        }
        
        /*
         * When only one part is given, the value is stored directly in
         * the network address without any byte rearrangement.
         *
         * When a two part address is supplied, the last part is
         * interpreted as a 24-bit quantity and placed in the right
         * most three bytes of the network address. This makes the
         * two part address format convenient for specifying Class A
         * network addresses as net.host.
         *
         * When a three part address is specified, the last part is
         * interpreted as a 16-bit quantity and placed in the right
         * most two bytes of the network address. This makes the
         * three part address format convenient for specifying
         * Class B net- work addresses as 128.net.host.
         *
         * When four parts are specified, each is interpreted as a
         * byte of data and assigned, from left to right, to the
         * four bytes of an IPv4 address.
         *
         * We determine and parse the leading parts, if any, as single
         * byte values in one pass directly into the resulting byte[],
         * then the remainder is treated as a 8-to-32-bit entity and
         * translated into the remaining bytes in the array.
         */
        for(int i = 0; i<len; i++) {
            char c = ip4.charAt(i);
            if(c == '.') {
                if(newOctet || tmpValue<0 || tmpValue>0xff || currByte == 3) {
                    return null;
                }
                res[currByte++] = (byte) (tmpValue & 0xff);
                tmpValue = 0;
                newOctet = true;
            } else {
                int digit = Character.digit(c, 10);
                if(digit<0) {
                    return null;
                }
                tmpValue *= 10;
                tmpValue += digit;
                newOctet = false;
            }
        }
        
        if(newOctet || tmpValue<0 || tmpValue >= (1L << ((4 - currByte) * 8))) {
            return null;
        }
        
        switch(currByte) {
            case 0:
                res[0] = (byte) ((tmpValue >> 24) & 0xff);
            case 1:
                res[1] = (byte) ((tmpValue >> 16) & 0xff);
            case 2:
                res[2] = (byte) ((tmpValue >> 8) & 0xff);
            case 3:
                res[3] = (byte) ((tmpValue >> 0) & 0xff);
        }
        
        return res;
    }
    
    /**
     * Convert IPv6 presentation level address to network order binary form.
     * credit:
     * Converted from C code from Solaris 8 (inet_pton)
     *
     * Any component of the string following a per-cent % is ignored.
     *
     * @param src a String representing an IPv6 address in textual format
     *
     * @return a byte array representing the IPv6 numeric address
     */
    // 将文本形式的IP6地址转换为二进制形式
    public static byte[] textToNumericFormatV6(String ip6) {
        // Shortest valid string is "::", hence at least 2 chars
        if(ip6.length()<2) {
            return null;
        }
        
        int colonp;
        char ch;
        boolean saw_xdigit;
        int val;
        char[] srcb = ip6.toCharArray();
        byte[] dst = new byte[INADDR16SZ];
        
        int srcb_length = srcb.length;
        int pc = ip6.indexOf('%');
        if(pc == srcb_length - 1) {
            return null;
        }
        
        if(pc != -1) {
            srcb_length = pc;
        }
        
        colonp = -1;
        int i = 0, j = 0;
        /* Leading :: requires some special handling. */
        if(srcb[i] == ':') {
            if(srcb[++i] != ':') {
                return null;
            }
        }
        
        int curtok = i;
        saw_xdigit = false;
        val = 0;
        
        while(i<srcb_length) {
            ch = srcb[i++];
            int chval = Character.digit(ch, 16);
            if(chval != -1) {
                val <<= 4;
                val |= chval;
                if(val>0xffff)
                    return null;
                saw_xdigit = true;
                continue;
            }
            
            if(ch == ':') {
                curtok = i;
                if(!saw_xdigit) {
                    if(colonp != -1) {
                        return null;
                    }
                    colonp = j;
                    continue;
                } else if(i == srcb_length) {
                    return null;
                }
                
                if(j + INT16SZ>INADDR16SZ) {
                    return null;
                }
                dst[j++] = (byte) ((val >> 8) & 0xff);
                dst[j++] = (byte) (val & 0xff);
                saw_xdigit = false;
                val = 0;
                continue;
            }
            
            if(ch == '.' && ((j + INADDR4SZ)<=INADDR16SZ)) {
                String ia4 = ip6.substring(curtok, srcb_length);
                /* check this IPv4 address has 3 dots, ie. A.B.C.D */
                int dot_count = 0, index = 0;
                while((index = ia4.indexOf('.', index)) != -1) {
                    dot_count++;
                    index++;
                }
                
                if(dot_count != 3) {
                    return null;
                }
                
                byte[] v4addr = textToNumericFormatV4(ia4);
                if(v4addr == null) {
                    return null;
                }
                
                for(int k = 0; k<INADDR4SZ; k++) {
                    dst[j++] = v4addr[k];
                }
                saw_xdigit = false;
                break;  /* '\0' was seen by inet_pton4(). */
            }
            
            return null;
        }
        
        if(saw_xdigit) {
            if(j + INT16SZ>INADDR16SZ)
                return null;
            dst[j++] = (byte) ((val >> 8) & 0xff);
            dst[j++] = (byte) (val & 0xff);
        }
        
        if(colonp != -1) {
            int n = j - colonp;
            
            if(j == INADDR16SZ)
                return null;
            for(i = 1; i<=n; i++) {
                dst[INADDR16SZ - i] = dst[colonp + n - i];
                dst[colonp + n - i] = 0;
            }
            j = INADDR16SZ;
        }
        
        if(j != INADDR16SZ) {
            return null;
        }
        
        // 将IP6地址转换为IP4地址，转换失败则返回null
        byte[] newdst = convertFromIPv4MappedAddress(dst);
        
        return newdst != null ? newdst : dst;
    }
    
    /**
     * @param src a String representing an IPv4 address in textual format
     *
     * @return a boolean indicating whether src is an IPv4 literal address
     */
    // 判断指定的地址是否为IP4地址
    public static boolean isIPv4LiteralAddress(String ip4) {
        return textToNumericFormatV4(ip4) != null;
    }
    
    /**
     * @param src a String representing an IPv6 address in textual format
     *
     * @return a boolean indicating whether src is an IPv6 literal address
     */
    // 判断指定的地址是否为IP6地址
    public static boolean isIPv6LiteralAddress(String ip6) {
        return textToNumericFormatV6(ip6) != null;
    }
    
    /**
     * Convert IPv4-Mapped address to IPv4 address. Both input and
     * returned value are in network order binary form.
     *
     * @param addr a String representing an IPv4-Mapped address in textual format
     *
     * @return a byte array representing the IPv4 numeric address
     */
    // 将IP6地址转换为IP4地址，转换失败则返回null
    public static byte[] convertFromIPv4MappedAddress(byte[] addr) {
        // 指定的IP地址是IP4地址映射成的IP6地址
        if(isIPv4MappedAddress(addr)) {
            // 将IP6地址转换为IP4地址
            byte[] newAddr = new byte[INADDR4SZ];
            System.arraycopy(addr, 12, newAddr, 0, INADDR4SZ);
            return newAddr;
        }
        
        return null;
    }
    
    /**
     * Utility routine to check if the InetAddress is an
     * IPv4 mapped IPv6 address.
     *
     * @return a <code>boolean</code> indicating if the InetAddress is
     * an IPv4 mapped IPv6 address; or false if address is IPv4 address.
     */
    /*
     * 判断指定的IP地址是否为IP4地址映射成的IP6地址
     *
     * 格式为0000:0000:0000:0000:0000:FFFF:XXXX:XXXX
     * 其中，后4个字节XXXX:XXXX是映射前的IP4地址
     */
    private static boolean isIPv4MappedAddress(byte[] addr) {
        if(addr.length<INADDR16SZ) {
            return false;
        }
        return (addr[0] == 0x00) && (addr[1] == 0x00) && (addr[2] == 0x00) && (addr[3] == 0x00) && (addr[4] == 0x00) && (addr[5] == 0x00) && (addr[6] == 0x00) && (addr[7] == 0x00) && (addr[8] == 0x00) && (addr[9] == 0x00) && (addr[10] == (byte) 0xff) && (addr[11] == (byte) 0xff);
    }
    
}
