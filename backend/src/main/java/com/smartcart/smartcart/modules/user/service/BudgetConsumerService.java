package com.smartcart.smartcart.modules.user.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.smartcart.smartcart.modules.user.dto.BudgetAlertEvent;

@Service
public class BudgetConsumerService {

    @KafkaListener(topics = "budget-alerts", groupId = "smartcart-group")
    public void listen(BudgetAlertEvent event) {
        // Aquí podrías guardar una notificación en base de datos o enviar un email
        System.out.printf("⚠️ [CONSUMER] El usuario %d ha excedido su límite %s. Total: %.2f / Límite: %.2f%n",
                event.userId(), event.type(), event.currentTotal(), event.limitAmount());
    }
}