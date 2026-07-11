package com.weib.repository;

import com.weib.entity.NotificationEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {
    Optional<NotificationEvent> findByEventId(String eventId);
    List<NotificationEvent> findByRecipientIdAndRecipientRoleAndIdGreaterThanOrderByIdAsc(
            Long recipientId, String recipientRole, Long afterId, Pageable pageable);
    Optional<NotificationEvent> findByIdAndRecipientIdAndRecipientRole(Long id, Long recipientId, String recipientRole);
}
