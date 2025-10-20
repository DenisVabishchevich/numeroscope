package com.numeroscope.bot.internal;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@RequiredArgsConstructor(onConstructor_ = @ConstructorBinding)
@ConfigurationProperties(prefix = "telegram.bot.numeroscope")
public class NumeroscopeProperties {

    @NotEmpty
    private final String botPaymentToken;
    @NotEmpty
    private final String botUsername;
    @NotEmpty
    private final String botToken;
}
