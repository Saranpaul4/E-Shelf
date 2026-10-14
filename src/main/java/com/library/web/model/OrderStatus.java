package com.library.web.model;

public enum OrderStatus {

    PAID("Confirmed"),
    RETURNED("Returned"),
    CANCELLED("Cancelled");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
