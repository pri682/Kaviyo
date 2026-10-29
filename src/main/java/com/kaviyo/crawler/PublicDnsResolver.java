package com.kaviyo.crawler;

import org.apache.hc.client5.http.DnsResolver;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Locale;

/**
 * Validates every DNS answer at the point Apache HttpClient resolves a connection.
 * Returning only checked addresses prevents the HTTP client from doing an unchecked second lookup.
 */
public final class PublicDnsResolver implements DnsResolver {
    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
        String normalizedHost = normalizeHost(host);
        if (isLocalName(normalizedHost)) {
            throw new BlockedDestinationException(normalizedHost);
        }

        InetAddress[] addresses = InetAddress.getAllByName(normalizedHost);
        if (addresses.length == 0 || Arrays.stream(addresses).anyMatch(address -> !isPublicAddress(address))) {
            throw new BlockedDestinationException(normalizedHost);
        }
        return addresses;
    }

    @Override
    public String resolveCanonicalHostname(String host) {
        return host;
    }

    static String normalizeHost(String host) throws BlockedDestinationException {
        if (host == null || host.isBlank() || host.indexOf('%') >= 0) {
            throw new BlockedDestinationException(String.valueOf(host));
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isBlank() || (!normalized.contains(".") && !normalized.contains(":"))) {
            throw new BlockedDestinationException(normalized);
        }
        return normalized;
    }

    static boolean isLocalName(String host) {
        return host.equals("localhost")
                || host.endsWith(".localhost")
                || host.endsWith(".local")
                || host.endsWith(".internal")
                || host.endsWith(".invalid")
                || host.endsWith(".test")
                || host.endsWith(".example");
    }

    static boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }
        if (address instanceof Inet4Address) {
            return isPublicIpv4(address.getAddress());
        }
        if (address instanceof Inet6Address ipv6 && ipv6.getScopeId() != 0) {
            return false;
        }

        byte[] bytes = address.getAddress();
        if (bytes.length != 16) {
            return false;
        }
        if (isIpv4MappedIpv6(bytes)) {
            try {
                return isPublicIpv4(Arrays.copyOfRange(bytes, 12, 16));
            } catch (RuntimeException ignored) {
                return false;
            }
        }

        // Global unicast IPv6 space is 2000::/3. Reject documentation allocations too.
        return (bytes[0] & 0xE0) == 0x20
                && !(Byte.toUnsignedInt(bytes[0]) == 0x20
                && Byte.toUnsignedInt(bytes[1]) == 0x01
                && Byte.toUnsignedInt(bytes[2]) == 0x0D
                && Byte.toUnsignedInt(bytes[3]) == 0xB8);
    }

    private static boolean isPublicIpv4(byte[] address) {
        if (address.length != 4) {
            return false;
        }
        int a = Byte.toUnsignedInt(address[0]);
        int b = Byte.toUnsignedInt(address[1]);
        int c = Byte.toUnsignedInt(address[2]);
        if (a == 0 || a == 10 || a == 127 || a >= 224) {
            return false;
        }
        if (a == 100 && b >= 64 && b <= 127) {
            return false;
        }
        if (a == 169 && b == 254) {
            return false;
        }
        if (a == 172 && b >= 16 && b <= 31) {
            return false;
        }
        if (a == 192 && (b == 168 || (b == 0 && c == 0) || (b == 0 && c == 2) || (b == 88 && c == 99))) {
            return false;
        }
        if (a == 198 && ((b == 18 || b == 19) || (b == 51 && c == 100))) {
            return false;
        }
        return !(a == 203 && b == 0 && c == 113);
    }

    private static boolean isIpv4MappedIpv6(byte[] bytes) {
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return bytes[10] == (byte) 0xFF && bytes[11] == (byte) 0xFF;
    }
}
