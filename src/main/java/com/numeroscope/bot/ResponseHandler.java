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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ResponseHandler {

    private final SilentSender sender;
    private final Map<Long, UserState> chatStates;
    private final String paymentToken;
    private final DishRecipeRepository dishRecipeRepository;

    public ResponseHandler(SilentSender sender,
                           DBContext db,
                           String paymentToken,
                           DishRecipeRepository dishRecipeRepository) {
        this.sender = sender;
        this.paymentToken = paymentToken;
        chatStates = db.getMap("chat_states");
        this.dishRecipeRepository = dishRecipeRepository;
    }

    public void replyToStart(Long chatId) {

        sendAvailableRecipes(chatId);
        chatStates.put(chatId, UserState.STARTED);
    }

    private void sendAvailableRecipes(Long chatId) {
        log.info("send available recipes: {}", chatId);

        final var namePerRow = dishRecipeRepository.findAllUniqueNames()
            .stream()
            .map(n -> new KeyboardRow(List.of(new KeyboardButton(n))))
            .collect(Collectors.toSet());

        final var message = SendMessage.builder()
                .chatId(chatId)
            .text("Choose your dish recipe")
            .replyMarkup(ReplyKeyboardMarkup.builder()
                .keyboard(namePerRow)
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
        log.info("send invoice for {}", recipe.getUniqueName());

        SendInvoice invoice = SendInvoice.builder()
            .chatId(chatId)
            .currency("USD")
            .price(new LabeledPrice(recipe.getUniqueName(), recipe.getPrice()))
            .title("The " + recipe.getUniqueName() + " recipe")
            .description(recipe.getDescription())
            .payload(RandomStringUtils.insecure().nextAlphabetic(10))
            .photoUrl(recipe.getImageUrl())
            .providerToken(paymentToken)
            .needEmail(true)
            .isFlexible(false)
            .build();

        sender.execute(invoice);

        clearKeyboard(chatId);
    }

    private void clearKeyboard(Long chatId) {
        sender.execute(SendMessage.builder()
            .chatId(chatId)
            .replyMarkup(ReplyKeyboardRemove.builder()
                .removeKeyboard(true)
                .build())
            .build());
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

        SendInvoice invoice = SendInvoice.builder()
                .chatId(context.chatId())
                .currency("USD")
                .price(new LabeledPrice("Product A", 5000))
                .title("Invoice title")
                .description("Simple invoice")
                .payload(RandomStringUtils.insecure().nextAlphabetic(10))
                .startParameter("test-payment")
                .photoUrl("https://numero-bot-images.eu-central-1.linodeobjects.com/pngtree-funny-smile-icon-image-png-image_14976892.png")
                .providerToken(paymentToken)
                .needName(true)
                .needEmail(true)
                .isFlexible(false)
                .build();

        sender.execute(invoice);
    }
}