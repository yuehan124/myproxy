package com.myproxy.config;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Network utility class for MyProxy application.
 * 
 * @author yuehan124@gmail.com
 * @since 2026-07-05
 */
public final class NetUtils {

    private NetUtils() {
    }

    /**
     * Get the local non-loopback IPv4 address.
     * Iterates network interfaces and returns the first valid IPv4 address.
     *
     * @return local IPv4 address, or "127.0.0.1" if not found
     */
    public static String getLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.isLoopbackAddress()) continue;
                    String hostAddress = addr.getHostAddress();
                    if (hostAddress != null && !hostAddress.contains(":")) {
                        return hostAddress;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "127.0.0.1";
    }
}
