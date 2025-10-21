package com.numeroscope.payment.internal;

public enum TransactionStatus {
    NEW, // transaction created and sent to user
    PRE_CHECKOUT, // transaction received by user, but not yet confirmed by payment provider
    COMPLETED, // transaction confirmed by payment provider
    FAILED,
    REFUNDED
}
