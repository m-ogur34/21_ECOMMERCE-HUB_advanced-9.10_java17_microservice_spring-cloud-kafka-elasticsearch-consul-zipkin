package com.ecommerce.notification.repository;

import com.ecommerce.notification.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findByRecipientEmail(String email);
    List<NotificationLog> findByOrderId(Long orderId);
    List<NotificationLog> findByStatus(NotificationLog.NotificationStatus status);
}
