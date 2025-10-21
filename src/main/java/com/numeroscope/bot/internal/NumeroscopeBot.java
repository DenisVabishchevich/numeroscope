package com.numeroscope.bot.internal;

import org.mapdb.DBMaker;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.bot.BaseAbilityBot;
import org.telegram.abilitybots.api.db.MapDBContext;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Flag;
import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static org.telegram.abilitybots.api.objects.Locality.USER;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Component
public class NumeroscopeBot extends AbilityBot {

    private final ResponseHandler responseHandler;

    public NumeroscopeBot(NumeroscopeProperties properties,
                          DishRecipeRepository dishRecipeRepository,
                          TransactionEventPublisher eventPublisher) {
        // TODO use persistent storage
        MapDBContext db = new MapDBContext(DBMaker.heapDB()
//                .fileMmapEnable()
//                .checksumHeaderBypass()
            .make());
        super(properties.getBotToken(), properties.getBotUsername(), db);
        this.responseHandler = new ResponseHandler(
            silent(),
            db,
            properties.getBotPaymentToken(),
            dishRecipeRepository,
            eventPublisher
        );
    }

    @Override
    public long creatorId() {
        return 1L;
    }

    @SuppressWarnings("unused")
    public Ability startBot() {
        return Ability.builder()
            .name("start")
            .info("Start")
            .locality(USER)
            .privacy(PUBLIC)
            .action(context -> responseHandler.replyToStart(context.chatId()))
            .build();
    }

    @SuppressWarnings("unused")
    public Ability resetBot() {
        return Ability.builder()
            .name("reset")
            .info("Reset")
            .locality(USER)
            .privacy(PUBLIC)
            .action(context -> responseHandler.resetBot(context.chatId()))
            .build();
    }

    @SuppressWarnings("unused")
    public Reply replyToMessage() {
        final BiConsumer<BaseAbilityBot, Update> action = (BaseAbilityBot abilityBot, Update upd) ->
            responseHandler.replyToMessage(upd);

        return Reply.of(action, Flag.MESSAGE, notSlashMessage());
    }

    private Predicate<Update> notSlashMessage() {
        return upd -> upd.hasMessage() && upd.getMessage().hasText() && !upd.getMessage().getText().startsWith("/");
    }

    @SuppressWarnings("unused")
    public Reply replyToPreCheckout() {
        final BiConsumer<BaseAbilityBot, Update> action = (BaseAbilityBot abilityBot, Update upd) ->
            responseHandler.replyToPreCheckout(upd);

        return Reply.of(action, Flag.PRECHECKOUT_QUERY);
    }

    @SuppressWarnings("unused")
    public Reply replyToSuccessPayment() {
        final BiConsumer<BaseAbilityBot, Update> action = (BaseAbilityBot abilityBot, Update upd) ->
            responseHandler.replyToSuccessPayment(upd);

        final Predicate<Update> isNotCommand = upd -> upd.getMessage().hasSuccessfulPayment();

        return Reply.of(action, Flag.MESSAGE, isNotCommand);
    }
}