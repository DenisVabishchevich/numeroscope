package com.numeroscope;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class NumeroscopeApplicationTests extends AbstractIntegrationTest {

    @Test
    void verifyModulithSetup() {
        // Arrange
        ApplicationModules modules = ApplicationModules.of(NumeroscopeApplication.class);

        // Act & Assert
        modules.verify();
    }

}
