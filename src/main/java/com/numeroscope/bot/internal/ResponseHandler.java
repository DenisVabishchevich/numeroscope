package com.numeroscope.bot.internal;


import com.numeroscope.bot.TransactionDto;
import com.numeroscope.bot.TransactionStatus;
import lombok.extern.slf4j.Slf4j;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
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

    public void replyToMessage(Update update) {

        final var recipe = update.getMessage().getText().trim();
        final var dishRecipeOpt = dishRecipeRepository.findByUniqueName(recipe);

        dishRecipeOpt.ifPresentOrElse(
            r -> sendInvoice(r, update),
            () -> sendAvailableRecipes(update.getMessage().getChatId())
        );

    }

    private void sendInvoice(DishRecipeEntity recipe, Update update) {

        final var transactionId = UUID.randomUUID();
        final var currency = "USD";
        final var price = recipe.getPrice();

        eventPublisher.publishTransaction(TransactionDto.builder()
            .status(TransactionStatus.PENDING)
            .transactionAmount(Long.valueOf(price))
            .itemId(recipe.getId())
            .transactionCurrency(currency)
            .uuid(transactionId)
            .username(update.getMessage().getFrom().getUserName())
            .build());

        final var invoice = SendInvoice.builder()
            .chatId(update.getMessage().getChatId())
            .currency(currency)
            .price(new LabeledPrice(recipe.getUniqueName(), price))
            .title(recipe.getUniqueName())
            .description(recipe.getDescription())
            .payload(transactionId + ";" + recipe.getId())
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

    public void replyToPreCheckout(Update upd) {

        final var queryId = upd.getPreCheckoutQuery().getId();

        final var query = AnswerPreCheckoutQuery.builder()
            .preCheckoutQueryId(queryId)
            .ok(true)
            .build();

        sender.execute(query);
    }

    public void replyToSuccessPayment(Update upd) {
        final var chatId = upd.getMessage().getChatId();
        final var payload = upd.getMessage().getSuccessfulPayment().getInvoicePayload();
        final var dishId = payload.split(";")[1];
        final var transactionId = payload.split(";")[0];

        eventPublisher.updateTransactionStatus(transactionId, TransactionStatus.COMPLETED);

        final var dishRecipeEntity = dishRecipeRepository.findById(Long.valueOf(dishId))
            .orElseThrow(() -> new IllegalStateException("Dish not found"));

        final var message = SendMessage.builder()
            .chatId(chatId)
            .text(
                "Ingredients:" + System.lineSeparator() + dishRecipeEntity.getIngredients() + System.lineSeparator() +
                "Recipe:" + System.lineSeparator() + dishRecipeEntity.getRecipe()
            )
            .parseMode("Markdown")
            .build();

        sender.execute(message);
    }
}