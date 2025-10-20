package com.numeroscope.bot;

import com.numeroscope.AbstractIntegrationTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import java.time.Duration;
import java.util.UUID;

import static com.numeroscope.bot.TransactionDto.TransactionStatus.PENDING;

@ApplicationModuleTest
class TransactionEventPublisherTest extends AbstractIntegrationTest {

    @Autowired
    private TransactionEventPublisher publisher;

    @Test
    void publishTransactionEventSuccess(Scenario scenario) {
        // Arrange
        TransactionDto dto = TransactionDto.builder()
            .uuid(UUID.randomUUID())
            .status(PENDING)
            .build();

        // Act and Assert
        scenario.stimulate(() -> publisher.publishTransaction(dto))
            .andWaitAtMost(Duration.ofSeconds(5))
            .forEventOfType(TransactionDto.class)
            .toArriveAndVerify(event -> {
                Assertions.assertThat(event.getStatus()).isEqualTo(PENDING);
                Assertions.assertThat(event.getUuid()).isEqualTo(dto.getUuid());
            });


    }
}