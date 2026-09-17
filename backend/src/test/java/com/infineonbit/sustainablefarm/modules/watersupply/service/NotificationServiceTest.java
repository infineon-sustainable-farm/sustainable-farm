package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.NotificationCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Notification;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.NotificationRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void createRejectsUnknownUser() {
        UUID userId = UUID.randomUUID();
        NotificationCreateRequest notification = new NotificationCreateRequest(
            userId, "Title", "Message", "warning", false, null);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        NotificationService service = service();

        assertThrows(NotFoundException.class, () -> service.create(notification));
        verify(notificationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void markAllAsReadUpdatesEveryNotification() {
        Notification first = new Notification();
        Notification second = new Notification();
        first.setRead(false);
        second.setRead(false);
        List<Notification> notifications = List.of(first, second);
        when(notificationRepository.findAll()).thenReturn(notifications);
        when(notificationRepository.saveAll(notifications)).thenReturn(notifications);
        NotificationService service = service();

        service.markAllAsRead();

        verify(notificationRepository).saveAll(notifications);
        org.junit.jupiter.api.Assertions.assertTrue(first.getRead());
        org.junit.jupiter.api.Assertions.assertTrue(second.getRead());
    }

    private NotificationService service() {
        return new NotificationService(notificationRepository, userRepository);
    }
}