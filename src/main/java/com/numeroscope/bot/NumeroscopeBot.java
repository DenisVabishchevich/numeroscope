package com.numeroscope.bot;

import org.mapdb.DBMaker;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.bot.BaseAbilityBot;
import org.telegram.abilitybots.api.db.MapDBContext;
import org.telegram.abilitybots.api.objects.Ability;
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
                          DishRecipeRepository dishRecipeRepository) {
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
                dishRecipeRepository
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
        {
            final var message = upd.getMessage();
            final var chatId = message.getChatId();
            responseHandler.replyToMessage(chatId, message);
        };

        // Only trigger for non-command text messages
        final Predicate<Update> isNotCommand = upd -> !upd.getMessage().getText().startsWith("/");

        return Reply.of(action, isNotCommand);
    }
}