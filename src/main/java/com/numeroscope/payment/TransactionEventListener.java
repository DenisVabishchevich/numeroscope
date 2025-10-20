package com.numeroscope.payment;

import com.numeroscope.bot.TransactionDto;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventListener {

    @ApplicationModuleListener
    public void handleTransactionEvent(TransactionDto dto) {
        System.out.println("Received transaction event: " + dto);
    }
}
