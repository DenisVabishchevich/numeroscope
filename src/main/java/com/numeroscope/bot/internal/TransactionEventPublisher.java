package com.numeroscope.bot.internal;

import com.numeroscope.bot.TransactionDto;
import com.numeroscope.bot.TransactionStatus;
import com.numeroscope.bot.TransactionStatusUpdateDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionEventPublisher {

    private final ApplicationEventPublisher publisher;

    @Transactional
    public void publishTransaction(TransactionDto dto) {
        publisher.publishEvent(dto);
    }

    @Transactional
    public void updateTransactionStatus(String transactionId,
                                        TransactionStatus transactionStatus) {
        publisher.publishEvent(TransactionStatusUpdateDto.builder()
            .uuid(UUID.fromString(transactionId))
            .status(transactionStatus)
            .build());
    }
}
