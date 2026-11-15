package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.Notification;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.NotificationCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.NotificationResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.PageResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.NotificationRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public List<NotificationResponse> findAll() {
        return notificationRepository.findAll().stream().map(NotificationResponse::from).toList();
    }

    public PageResponse<NotificationResponse> findAll(Pageable pageable, UUID userId) {
        Page<Notification> page = userId == null
                ? notificationRepository.findAll(pageable)
                : notificationRepository.findByUserId(userId, pageable);
        return new PageResponse<>(page.map(NotificationResponse::from).getContent(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public NotificationResponse create(NotificationCreateRequest request) {
        requireUser(request.userId());
        Notification notification = new Notification();
        notification.setUserId(request.userId());
        notification.setTitle(request.title());
        notification.setMessage(request.message());
        notification.setType(request.type());
        notification.setRead(request.read() == null ? false : request.read());
        notification.setActionUrl(request.actionUrl());
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    public NotificationResponse get(UUID notificationId) {
        return NotificationResponse.from(getEntity(notificationId));
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId) {
        Notification notification = getEntity(notificationId);
        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public List<NotificationResponse> markAllAsRead() {
        List<Notification> notifications = notificationRepository.findAll();
        notifications.forEach(notification -> notification.setRead(true));
        return notificationRepository.saveAll(notifications).stream().map(NotificationResponse::from).toList();
    }

    @Transactional
    public void delete(UUID notificationId) {
        notificationRepository.delete(getEntity(notificationId));
    }

    private Notification getEntity(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification"));
    }

    private void requireUser(UUID userId) {
        if (userId == null || userRepository.findById(userId).isEmpty()) {
            throw new NotFoundException("User");
        }
    }
}