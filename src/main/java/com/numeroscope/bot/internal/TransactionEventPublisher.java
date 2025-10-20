package com.numeroscope.bot.internal;

import com.numeroscope.bot.TransactionDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionEventPublisher {

    private final ApplicationEventPublisher publisher;

    @Transactional
    public void publishTransaction(TransactionDto dto) {
        publisher.publishEvent(dto);
    }
}
