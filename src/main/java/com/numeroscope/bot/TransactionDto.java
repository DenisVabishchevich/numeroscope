package com.numeroscope.bot;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class TransactionDto {

    UUID uuid;

    TransactionStatus status;


    public enum TransactionStatus {
        PENDING,
        COMPLETED,
        FAILED,
        REFUNDED
    }
}
