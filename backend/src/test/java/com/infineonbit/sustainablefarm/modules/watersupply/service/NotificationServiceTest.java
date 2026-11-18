package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.NotificationCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Notification;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.NotificationRepository;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import com.infineonbit.sustainablefarm.modules.staff.api.InMemoryPersonDirectory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private NotificationRepository notificationRepository;

    private final PersonDirectory personDirectory = new InMemoryPersonDirectory();

    @Test
    void createRejectsUnknownUser() {
        Long userId = 4_242L;
        NotificationCreateRequest notification = new NotificationCreateRequest(
            userId, "Title", "Message", "warning", false, null);
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

    @Test
    void createAcceptsAPersonOfTheStaffReferential() {
        Long userId = personDirectory.idOf("Awa Demo");
        when(notificationRepository.save(org.mockito.ArgumentMatchers.any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var created = service().create(new NotificationCreateRequest(userId, "Title", "Message", "info", false, null));

        org.junit.jupiter.api.Assertions.assertEquals(userId, created.userId());
    }

    private NotificationService service() {
        return new NotificationService(notificationRepository, personDirectory);
    }
}