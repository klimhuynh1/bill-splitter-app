package com.mnfll.bill_splitter_app;

public enum PaymentStatus {
    YES("y"),
    NO("n");

    private final String code;

    PaymentStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
