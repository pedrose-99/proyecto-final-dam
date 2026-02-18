package com.smartcart.smartcart.modules.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartcart.smartcart.modules.group.entity.Group;
import com.smartcart.smartcart.modules.notification.entity.Notification;
import com.smartcart.smartcart.modules.user.entity.User;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);

    List<Notification> findByRecipientAndIsReadFalseOrderByCreatedAtDesc(User recipient);

    long countByRecipientAndIsReadFalse(User recipient);

    void deleteByRelatedGroup(Group relatedGroup);
}
