package com.numeroscope;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.modulith.Modulithic;

@Modulithic(sharedModules = "common")
@SpringBootApplication
@ConfigurationPropertiesScan
public class NumeroscopeApplication {

    public static void main(String[] args) {
		SpringApplication.run(NumeroscopeApplication.class, args);
	}

}
