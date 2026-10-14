package com.library.web.model;

public enum OrderType {

    BUY("Buy"),
    RENT("Rent");

    private final String label;

    OrderType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
