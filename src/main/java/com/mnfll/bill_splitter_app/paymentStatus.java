package com.mnfll.bill_splitter_app;

public enum paymentStatus {
    YES("y"),
    NO("n");

    private final String code;

    paymentStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
