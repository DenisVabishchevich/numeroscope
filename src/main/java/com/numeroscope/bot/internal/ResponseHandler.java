package com.numeroscope.bot.internal;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.numeroscope.bot.TransactionDto;
import com.numeroscope.bot.TransactionStatus;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
import java.util.stream.Collectors;

@Slf4j
public class ResponseHandler {

    private final SilentSender sender;
    private final Map<Long, UserState> chatStates;
    private final String paymentToken;
    private final DishRecipeRepository dishRecipeRepository;
    private final TransactionEventPublisher eventPublisher;
    private final ObjectMapper mapper;

    public ResponseHandler(SilentSender sender,
                           DBContext db,
                           String paymentToken,
                           DishRecipeRepository dishRecipeRepository,
                           TransactionEventPublisher eventPublisher,
                           ObjectMapper mapper) {
        this.sender = sender;
        this.paymentToken = paymentToken;
        chatStates = db.getMap("chat_states");
        this.dishRecipeRepository = dishRecipeRepository;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
    }

    public void start(Long chatId) {

        sendAvailableRecipes(chatId);
        chatStates.put(chatId, UserState.STARTED);
    }

    private void sendAvailableRecipes(Long chatId) {

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
            .status(TransactionStatus.NEW)
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

    public void reset(Long chatId) {
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

    public void preCheckout(Update upd) {

        final var queryId = upd.getPreCheckoutQuery().getId();
        final var currency = upd.getPreCheckoutQuery().getCurrency();
        final var totalAmount = upd.getPreCheckoutQuery().getTotalAmount();
        final var username = upd.getPreCheckoutQuery().getFrom().getUserName();
        final var payload = upd.getPreCheckoutQuery().getInvoicePayload();
        final var dishId = payload.split(";")[1];
        final var transactionId = payload.split(";")[0];

        eventPublisher.publishTransaction(TransactionDto.builder()
            .status(TransactionStatus.PRE_CHECKOUT)
            .transactionAmount(Long.valueOf(totalAmount))
            .itemId(Long.valueOf(dishId))
            .transactionCurrency(currency)
            .uuid(UUID.fromString(transactionId))
            .username(username)
            .build());

        final var query = AnswerPreCheckoutQuery.builder()
            .preCheckoutQueryId(queryId)
            .ok(true)
            .build();

        sender.execute(query);
    }

    @SneakyThrows
    public void successPayment(Update upd) {
        final var chatId = upd.getMessage().getChatId();
        final var payload = upd.getMessage().getSuccessfulPayment().getInvoicePayload();
        final var dishId = payload.split(";")[1];
        final var transactionId = payload.split(";")[0];
        final var currency = upd.getMessage().getSuccessfulPayment().getCurrency();
        final var totalAmount = upd.getMessage().getSuccessfulPayment().getTotalAmount();
        final var username = upd.getMessage().getFrom().getUserName();

        eventPublisher.publishTransaction(TransactionDto.builder()
            .status(TransactionStatus.COMPLETED)
            .transactionAmount(Long.valueOf(totalAmount))
            .itemId(Long.valueOf(dishId))
            .transactionCurrency(currency)
            .uuid(UUID.fromString(transactionId))
            .username(username)
            .build());

        final var dishRecipeEntity = dishRecipeRepository.findById(Long.valueOf(dishId))
            .orElseThrow(() -> new IllegalStateException("Dish not found"));

        final var messageText = buildRecipeMessage(dishRecipeEntity);

        final var message = SendMessage.builder()
            .chatId(chatId)
            .text(messageText)
            .parseMode("Markdown")
            .build();

        sender.execute(message);
    }

    @NotNull
    private String buildRecipeMessage(DishRecipeEntity dishRecipeEntity) throws JsonProcessingException {
        final var ingredientsMap = mapper.readValue(
            dishRecipeEntity.getIngredients(),
            new TypeReference<Map<String, Object>>() {
            }
        );

        final var formattedIngredients = ingredientsMap.entrySet().stream()
            .map(entry -> "- *" + entry.getKey() + "*: " + entry.getValue())
            .collect(Collectors.joining(System.lineSeparator()));

        final var recipeText = dishRecipeEntity.getRecipe();

        return """
            *🍽️ Ingredients:*
            %s
            
            *👩‍🍳 Recipe:*
            %s
            """.formatted(formattedIngredients, recipeText);
    }
}