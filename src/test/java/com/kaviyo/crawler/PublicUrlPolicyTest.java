package com.kaviyo.crawler;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PublicUrlPolicyTest {
    private final PublicDnsResolver resolver = new PublicDnsResolver();
    private final PublicUrlPolicy policy = new PublicUrlPolicy(resolver);

    @Test
    void blocksLocalAndReservedDestinations() {
        assertThrows(BlockedDestinationException.class, () -> resolver.resolve("127.0.0.1"));
        assertThrows(BlockedDestinationException.class, () -> resolver.resolve("10.0.0.1"));
        assertThrows(BlockedDestinationException.class, () -> resolver.resolve("169.254.169.254"));
        assertThrows(BlockedDestinationException.class, () -> resolver.resolve("192.0.2.10"));
        assertThrows(BlockedDestinationException.class, () -> resolver.resolve("::1"));
        assertThrows(BlockedDestinationException.class, () -> resolver.resolve("example.invalid"));
        assertThrows(BlockedDestinationException.class, () -> resolver.resolve("printer.local"));
    }

    @Test
    void permitsAValidatedPublicAddress() {
        assertDoesNotThrow(() -> policy.validate(URI.create("https://8.8.8.8/products/item")));
    }

    @Test
    void rejectsUnsupportedSchemesCredentialsAndNonstandardPorts() {
        assertThrows(PublicUrlPolicy.InvalidScanUrlException.class,
                () -> policy.validate(URI.create("file:///etc/passwd")));
        assertThrows(PublicUrlPolicy.InvalidScanUrlException.class,
                () -> policy.validate(URI.create("https://user:secret@8.8.8.8/product")));
        assertThrows(PublicUrlPolicy.InvalidScanUrlException.class,
                () -> policy.validate(URI.create("http://8.8.8.8:8080/product")));
    }

    @Test
    void rejectsEveryAddressIfDnsReturnsMixedPublicAndPrivateAnswers() throws Exception {
        InetAddress publicAddress = InetAddress.getByName("8.8.8.8");
        InetAddress privateAddress = InetAddress.getByName("10.0.0.1");
        org.junit.jupiter.api.Assertions.assertTrue(PublicDnsResolver.isPublicAddress(publicAddress));
        org.junit.jupiter.api.Assertions.assertFalse(PublicDnsResolver.isPublicAddress(privateAddress));
    }
}
