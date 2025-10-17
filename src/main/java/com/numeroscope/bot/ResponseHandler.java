package com.numeroscope.bot;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ResponseHandler {

    private final SilentSender sender;

    private final Map<Long, UserState> chatStates;

    public ResponseHandler(SilentSender sender, DBContext db) {
        this.sender = sender;
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
}