package com.numeroscope.payment.internal;

import com.numeroscope.bot.TransactionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionEventListener {

    private final TransactionRepository repository;

    @ApplicationModuleListener
    public void handleTransactionEvent(TransactionDto dto) {
        repository.save(TransactionEntity.builder()
            .uuid(dto.getUuid())
            .transactionAmount(dto.getTransactionAmount())
            .transactionCurrency(dto.getTransactionCurrency())
            .username(dto.getUsername())
            .itemId(dto.getItemId())
            .itemType(ItemType.valueOf(dto.getItemType().name()))
            .transactionStatus(TransactionStatus.valueOf(dto.getStatus().name()))
            .build());
    }
}
