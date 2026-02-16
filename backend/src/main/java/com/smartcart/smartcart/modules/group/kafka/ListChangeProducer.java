package com.smartcart.smartcart.modules.group.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ListChangeProducer {

    private static final String LIST_CHANGE_TOPIC = "list-change-topic";
    private static final String EMAIL_INVITE_TOPIC = "email-service-topic";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendListChangeEvent(ListChangeEvent event) {
        log.info("Enviando evento de cambio en lista: {} - acción: {}", event.listName(), event.action());
        kafkaTemplate.send(LIST_CHANGE_TOPIC, String.valueOf(event.groupId()), event);
    }

    public void sendEmailInviteEvent(EmailInviteEvent event) {
        log.info("Enviando evento de invitación por email a: {}", event.email());
        kafkaTemplate.send(EMAIL_INVITE_TOPIC, event.email(), event);
    }
}
