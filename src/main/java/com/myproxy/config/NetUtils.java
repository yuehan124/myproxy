package com.myproxy.config;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

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
     * Heuristically check whether a network interface is virtual
     * (e.g. Docker, VPN, loopback adapter, Hyper-V, VirtualBox).
     *
     * @param ni the network interface to check
     * @return {@code true} if the interface is likely virtual
     */
    private static boolean isVirtualInterface(NetworkInterface ni) {
        try {
            if (ni.isLoopback() || ni.isVirtual()) {
                return true;
            }
        } catch (Exception ignored) {
        }
        String name = ni.getName();
        String displayName = ni.getDisplayName() != null ? ni.getDisplayName() : name;
        String lower = (name + " " + displayName).toLowerCase();
        // Common virtual interface name patterns
        return lower.contains("docker")
                || lower.contains("veth")
                || lower.contains("br-")
                || lower.contains("vmnet")
                || lower.contains("virtualbox")
                || lower.contains("tun")
                || lower.contains("tap")
                || lower.contains("vpn")
                || lower.contains("ppp")
                || lower.contains("hyper-v")
                || lower.contains("v ethernet")
                || lower.contains("loopback");
    }

    /**
     * Collect non-loopback IPv4 addresses from a network interface.
     *
     * @param ni the network interface
     * @return list of IPv4 address strings (never {@code null})
     */
    private static List<String> collectIpv4(NetworkInterface ni) {
        List<String> result = new ArrayList<>();
        Enumeration<InetAddress> addresses = ni.getInetAddresses();
        while (addresses.hasMoreElements()) {
            InetAddress addr = addresses.nextElement();
            if (addr.isLoopbackAddress()) {
                continue;
            }
            String hostAddress = addr.getHostAddress();
            if (hostAddress != null && !hostAddress.contains(":")) {
                result.add(hostAddress);
            }
        }
        return result;
    }

    /**
     * Get the local non-loopback IPv4 address, preferring physical network
     * interfaces over virtual ones (e.g. Docker, VPN, Hyper-V, VirtualBox).
     * <p>
     * The selection order is:
     * <ol>
     *   <li>Physical (non-virtual) interfaces</li>
     *   <li>Virtual interfaces as fallback</li>
     * </ol>
     *
     * @return local IPv4 address, or "127.0.0.1" if not found
     */
    public static String getLocalIp() {
        List<String> virtualIps = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) {
                    continue;
                }
                List<String> ips = collectIpv4(ni);
                if (ips.isEmpty()) {
                    continue;
                }
                if (isVirtualInterface(ni)) {
                    // Keep virtual interface IPs as fallback
                    virtualIps.addAll(ips);
                } else {
                    // Return the first physical interface IP immediately
                    return ips.get(0);
                }
            }
        } catch (Exception ignored) {
        }
        // No physical interface found, fall back to virtual ones
        if (!virtualIps.isEmpty()) {
            return virtualIps.get(0);
        }
        return "127.0.0.1";
    }
}
