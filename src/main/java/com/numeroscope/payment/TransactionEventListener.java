package com.numeroscope.payment;

import com.numeroscope.bot.TransactionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionEventListener {

    @ApplicationModuleListener
    public void handleTransactionEvent(TransactionDto dto) {
        System.out.println("Received transaction event: " + dto);
    }
}
