package com.kaviyo.crawler;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;

/** Applies syntax, port, and public-address restrictions before an outbound request. */
public final class PublicUrlPolicy {
    private final PublicDnsResolver dnsResolver;

    public PublicUrlPolicy(PublicDnsResolver dnsResolver) {
        this.dnsResolver = dnsResolver;
    }

    public URI validate(URI input) throws InvalidScanUrlException, BlockedDestinationException {
        if (input == null || input.toString().length() > 2_048) {
            throw new InvalidScanUrlException("URL is missing or longer than 2,048 characters.");
        }
        URI uri;
        try {
            uri = new URI(input.toString().trim()).normalize();
        } catch (URISyntaxException exception) {
            throw new InvalidScanUrlException("URL is not valid.");
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null || uri.getUserInfo() != null
                || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new InvalidScanUrlException("Only public HTTP or HTTPS URLs without embedded credentials are allowed.");
        }
        int port = uri.getPort();
        int expectedPort = scheme.equalsIgnoreCase("https") ? 443 : 80;
        if (port != -1 && port != expectedPort) {
            throw new InvalidScanUrlException("Only the standard HTTP and HTTPS ports are allowed.");
        }

        String normalizedHost = PublicDnsResolver.normalizeHost(host);
        if (PublicDnsResolver.isLocalName(normalizedHost)) {
            throw new BlockedDestinationException(normalizedHost);
        }
        try {
            // Apache HttpClient uses this same validated resolver again at connect time, so a DNS
            // answer cannot change to a private address unnoticed.
            dnsResolver.resolve(normalizedHost);
        } catch (BlockedDestinationException exception) {
            throw exception;
        } catch (UnknownHostException exception) {
            throw new InvalidScanUrlException("The selected hostname could not be resolved.");
        }
        return uri;
    }

    boolean sameHost(URI first, URI second) {
        String firstHost = first.getHost();
        String secondHost = second.getHost();
        return firstHost != null && secondHost != null
                && firstHost.toLowerCase(Locale.ROOT).equals(secondHost.toLowerCase(Locale.ROOT));
    }

    public static final class InvalidScanUrlException extends Exception {
        public InvalidScanUrlException(String message) {
            super(message);
        }
    }
}
