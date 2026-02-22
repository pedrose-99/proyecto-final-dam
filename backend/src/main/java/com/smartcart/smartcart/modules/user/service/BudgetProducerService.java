package com.smartcart.smartcart.modules.user.service;


import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.smartcart.smartcart.modules.user.dto.BudgetAlertEvent;
import com.smartcart.smartcart.modules.user.entity.SpendingLimit;
import com.smartcart.smartcart.modules.user.repository.SpendingLimitRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SpendingLimitRepository limitRepository;

    private static final String TOPIC = "budget-alerts";

    public void checkLimitsAndNotify(Long userId, Double currentCartTotal) {
        List<SpendingLimit> activeLimits = limitRepository.findByIdUserAndIsActiveTrue(userId);

        for (SpendingLimit limit : activeLimits) {
            if (BigDecimal.valueOf(currentCartTotal).compareTo(limit.getAmount()) > 0) {

                BudgetAlertEvent event = new BudgetAlertEvent(
                    userId,
                    currentCartTotal,
                    limit.getAmount(),
                    limit.getType()
                );

                kafkaTemplate.send(TOPIC, userId.toString(), event);
                System.out.println("🚨 Alerta Kafka enviada para usuario: " + userId);
            }
        }
    }
}