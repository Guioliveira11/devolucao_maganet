package com.magalu.devolucao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // necessario para o QRCodeExpirationJob
public class DevolucaoMagaluApplication {
    public static void main(String[] args) {
        SpringApplication.run(DevolucaoMagaluApplication.class, args);
    }
}
