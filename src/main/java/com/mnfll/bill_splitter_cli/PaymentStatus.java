package com.mnfll.bill_splitter_cli;

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
