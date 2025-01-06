package com.tsqco.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationHelper {

    @Value("${telegram.botKey}")
    private String botKey;

    @Value("${telegram.chatID}")
    private String chatId;

    @Value("${telegram.appURL}")
    private String telegramAppURL;

    @Inject
    @Qualifier("Notification")
    private final WebClient webClient;

    private static NotificationHelper instance;

    @PostConstruct
    private void init() {
        instance = this;
    }

    public static void sendMessage(String message) {
        String endpoint = String.format("/bot%s/sendMessage", instance.botKey);
        instance.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(endpoint)
                        .queryParam("chat_id", instance.chatId)
                        .queryParam("text", message)
                        .queryParam("parse_mode", "Markdown")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> System.err.println("Failed to send message: " + e.getMessage()))
                .subscribe();
    }
}
