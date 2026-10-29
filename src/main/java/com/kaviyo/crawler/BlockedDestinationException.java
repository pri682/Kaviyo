package com.kaviyo.crawler;

import java.net.UnknownHostException;

/** Raised when a requested hostname resolves to an address that must not be fetched. */
public final class BlockedDestinationException extends UnknownHostException {
    public BlockedDestinationException(String host) {
        super("The selected URL resolves to a local, private, reserved, or unsupported destination: " + host);
    }
}
