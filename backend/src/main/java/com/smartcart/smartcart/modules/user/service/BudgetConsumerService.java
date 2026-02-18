package com.smartcart.smartcart.modules.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.smartcart.smartcart.common.enums.NotificationType;
import com.smartcart.smartcart.modules.notification.entity.Notification;
import com.smartcart.smartcart.modules.notification.repository.NotificationRepository;
import com.smartcart.smartcart.modules.user.dto.BudgetAlertEvent;
import com.smartcart.smartcart.modules.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class BudgetConsumerService
{

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @KafkaListener(topics = "budget-alerts", groupId = "smartcart-group")
    public void listen(BudgetAlertEvent event)
    {
        System.out.printf("⚠️ [CONSUMER] El usuario %d ha excedido su límite %s. Total: %.2f / Límite: %.2f%n",
                event.userId(), event.type(), event.currentTotal(), event.limitAmount());

        userRepository.findById(event.userId().intValue()).ifPresent(user ->
        {
            Notification notification = new Notification();
            notification.setRecipient(user);
            notification.setType(NotificationType.BUDGET_ALERT);
            notification.setMessage(String.format(
                    "Has excedido tu límite %s de %.2f€. Tu gasto actual es de %.2f€.",
                    event.type().name().toLowerCase(),
                    event.limitAmount(),
                    event.currentTotal()
            ));
            notification.setIsRead(false);
            notificationRepository.save(notification);
        });
    }
}
