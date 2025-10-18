package com.numeroscope.bot;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;

import java.util.List;
import java.util.Map;

@Slf4j
public class ResponseHandler {

    private final SilentSender sender;
    private final Map<Long, UserState> chatStates;
    private final String paymentToken;

    public ResponseHandler(SilentSender sender,
                    DBContext db,
                    String paymentToken) {
        this.sender = sender;
        this.paymentToken = paymentToken;
        chatStates = db.getMap("chat_states");
    }

    public void replyToStart(Long chatId) {
        chatStates.put(chatId, UserState.STARTED);
        final var message = SendMessage.builder()
                .chatId(chatId)
                .text("You are started")
                .build();
        sender.execute(message);
    }

    public void replyToMessage(Long chatId, Message message) {
        echoReply(chatId, message);
    }

    private void echoReply(Long chatId, Message input) {
        final var message = SendMessage.builder()
                .chatId(chatId)
                .protectContent(true) // no forwarding of messages
                .text("Echo: " + input.getText())
                .build();
        sender.execute(message);
    }


    public void resetBot(Long chatId) {
        chatStates.remove(chatId);
        final var message = SendMessage.builder()
                .chatId(chatId)
                .text("Your session has been reset.")
                .build();
        sender.execute(message);

    }

    public void pay(MessageContext context) {
        List<LabeledPrice> prices = List.of(
                new LabeledPrice("Product A", 5000),  // $50.00
                new LabeledPrice("Shipping", 1000)    // $10.00
        );

        SendInvoice invoice = SendInvoice.builder()
                .chatId(context.chatId())
                .currency("USD")
                .prices(prices)
                .title("Invoice title")
                .description("Simple invoice")
                .payload(RandomStringUtils.insecure().nextAlphabetic(10))
                .startParameter("test-payment")
                .providerToken(paymentToken)
                .needName(true)
                .needEmail(true)
                .isFlexible(false)
                .build();

        sender.execute(invoice);
    }
}