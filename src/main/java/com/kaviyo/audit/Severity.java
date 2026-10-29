package com.kaviyo.audit;

public enum Severity {
    HIGH(0),
    MEDIUM(1),
    LOW(2);

    private final int sortOrder;

    Severity(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public int sortOrder() {
        return sortOrder;
    }
}
