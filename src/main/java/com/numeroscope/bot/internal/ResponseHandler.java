package com.numeroscope.bot.internal;


import com.numeroscope.bot.TransactionDto;
import com.numeroscope.bot.UserState;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class ResponseHandler {

    private final SilentSender sender;
    private final Map<Long, UserState> chatStates;
    private final String paymentToken;
    private final DishRecipeRepository dishRecipeRepository;
    private final TransactionEventPublisher eventPublisher;

    public ResponseHandler(SilentSender sender,
                           DBContext db,
                           String paymentToken,
                           DishRecipeRepository dishRecipeRepository,
                           TransactionEventPublisher eventPublisher) {
        this.sender = sender;
        this.paymentToken = paymentToken;
        chatStates = db.getMap("chat_states");
        this.dishRecipeRepository = dishRecipeRepository;
        this.eventPublisher = eventPublisher;
    }

    public void replyToStart(Long chatId) {

        sendAvailableRecipes(chatId);
        chatStates.put(chatId, UserState.STARTED);
    }

    private void sendAvailableRecipes(Long chatId) {
        log.info("send available recipes: {}", chatId);

        final var keyboardButtons = new KeyboardRow();
        dishRecipeRepository.findAllUniqueNames()
            .forEach(keyboardButtons::add);

        final var message = SendMessage.builder()
            .chatId(chatId)
            .text("Choose your dish")
            .replyMarkup(ReplyKeyboardMarkup.builder()
                .keyboard(List.of(keyboardButtons))
                .oneTimeKeyboard(true)
                .build())
            .build();

        sender.execute(message);
    }

    public void replyToMessage(Long chatId, Message message) {
        log.info("reply to message {}", message.getText());

        final var recipe = message.getText().trim();
        final var dishRecipeOpt = dishRecipeRepository.findByUniqueName(recipe);

        dishRecipeOpt.ifPresentOrElse(
            r -> sendInvoice(r, chatId),
            () -> sendAvailableRecipes(chatId)
        );

    }

    private void sendInvoice(DishRecipeEntity recipe, Long chatId) {

        eventPublisher.publishTransaction(TransactionDto.builder()
            .status(TransactionDto.TransactionStatus.PENDING)
            .uuid(UUID.randomUUID())
            .build());

        SendInvoice invoice = SendInvoice.builder()
            .chatId(chatId)
            .currency("USD")
            .price(new LabeledPrice(recipe.getUniqueName(), recipe.getPrice()))
            .title(recipe.getUniqueName())
            .description(recipe.getDescription())
            .payload(RandomStringUtils.insecure().nextAlphabetic(10))
            .photoUrl(recipe.getImageUrl())
            .startParameter("startParameter")
            .photoWidth(512)
            .photoHeight(512)
            .providerToken(paymentToken)
            .maxTipAmount(100_000)
            .suggestedTipAmounts(List.of(100, 300, 500, 10000))
            .needEmail(true)
            .isFlexible(false)
            .build();

        sender.execute(invoice);

    }

    public void resetBot(Long chatId) {
        chatStates.remove(chatId);
        final var message = SendMessage.builder()
            .chatId(chatId)
            .text("Your session has been reset.")
            .replyMarkup(ReplyKeyboardRemove.builder()
                .removeKeyboard(true)
                .build())
            .build();
        sender.execute(message);

    }
}