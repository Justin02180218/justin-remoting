package com.justin.net.remoting.common;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public class RemotingUtil {
    public static String parseSocketAddressAddr(SocketAddress socketAddress) {
        if (socketAddress != null) {
            final String addr = socketAddress.toString();

            if (addr.length() > 0) {
                return addr.substring(1);
            }
        }
        return "UNKNOWN";
    }

    public static SocketAddress addr2SocketAddress(final String addr) {
        int i = addr.lastIndexOf(":");
        String host = addr.substring(0, i);
        String port = addr.substring(i + 1);
        InetSocketAddress inetSocketAddress = new InetSocketAddress(host, Integer.parseInt(port));
        return inetSocketAddress;
    }
}
