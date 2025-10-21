package com.numeroscope.bot;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class TransactionStatusUpdateDto {

    UUID uuid;

    TransactionStatus status;

}
