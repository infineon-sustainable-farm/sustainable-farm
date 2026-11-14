package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.regex.Pattern;

/**
 * The address a sign-in comes from, for the per-client limits.
 *
 * <p>nginx overwrites {@code X-Real-IP} with the address of the TCP connection it
 * received ({@code proxy_set_header X-Real-IP $remote_addr}), so a value sent by
 * the browser never survives. The header is only believed when the connection
 * itself comes from a loopback or private address (nginx in the Compose network,
 * or this machine: the backend port is bound to 127.0.0.1). A connection from a
 * public address is used as it is. Only literal IP addresses are accepted, so no
 * name is ever resolved.</p>
 */
public final class ClientAddress {

    public static final String HEADER = "X-Real-IP";
    private static final Pattern IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9A-Fa-f:.]{2,45}$");

    private ClientAddress() {
    }

    public static String of(HttpServletRequest request) {
        String peer = request.getRemoteAddr();
        String forwarded = request.getHeader(HEADER);
        if (forwarded != null && isLiteral(forwarded.trim()) && isInternal(peer)) {
            return forwarded.trim();
        }
        return peer == null ? "unknown" : peer;
    }

    static boolean isLiteral(String value) {
        return IPV4.matcher(value).matches() || (value.contains(":") && IPV6.matcher(value).matches());
    }

    static boolean isInternal(String address) {
        if (address == null || !isLiteral(address)) {
            return false;
        }
        try {
            InetAddress ip = InetAddress.getByName(address);
            return ip.isLoopbackAddress() || ip.isSiteLocalAddress() || ip.isLinkLocalAddress()
                    || (ip.getAddress().length == 16 && (ip.getAddress()[0] & 0xFE) == 0xFC);
        } catch (Exception e) {
            return false;
        }
    }
}
