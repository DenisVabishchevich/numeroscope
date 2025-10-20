package com.numeroscope.bot;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class TransactionDto {

    UUID uuid;
    TransactionStatus status;
    Long transactionAmount;
    String transactionCurrency;
    Long itemId;
    ItemType itemType;
    String username;

    public enum TransactionStatus {
        PENDING,
        COMPLETED,
        FAILED,
        REFUNDED
    }

    public enum ItemType {
        DISH_RECIPE
    }
}
