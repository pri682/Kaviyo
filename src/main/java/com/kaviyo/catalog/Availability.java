package com.kaviyo.catalog;

public enum Availability {
    IN_STOCK("In stock"),
    OUT_OF_STOCK("Out of stock"),
    PREORDER("Preorder"),
    UNKNOWN("Unknown");

    private final String displayName;

    Availability(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
