package com.numeroscope.bot;

import com.numeroscope.AbstractIntegrationTest;
import com.numeroscope.payment.TransactionRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;


class TransactionEventPublisherTest extends AbstractIntegrationTest {

    @Autowired
    private TransactionEventPublisher publisher;

    @Autowired
    private TransactionRepository repository;

    @Test
    void publishTransactionEventSuccess() {
        // Arrange
        TransactionDto dto = TransactionDto.builder()
            .uuid(UUID.randomUUID())
            .build();

        // Act
        publisher.initTransaction(dto);

        // Assert
        Assertions.assertThat(repository.findByUuid(dto.getUuid())).isNotEmpty();
    }
}