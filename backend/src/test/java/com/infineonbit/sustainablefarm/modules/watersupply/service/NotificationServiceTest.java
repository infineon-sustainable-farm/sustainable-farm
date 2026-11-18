package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.notification.api.AlertBoard;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertDetails;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertDto;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertSeverity;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertStatus;
import com.infineonbit.sustainablefarm.modules.notification.api.SourceModule;
import com.infineonbit.sustainablefarm.modules.staff.api.InMemoryPersonDirectory;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.NotificationCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.NotificationResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private AlertBoard alertBoard;

    private final PersonDirectory personDirectory = new InMemoryPersonDirectory();

    @Test
    void createRejectsUnknownUser() {
        Long userId = 4_242L;
        NotificationCreateRequest notification = new NotificationCreateRequest(
            userId, "Title", "Message", "warning", false, null);
        NotificationService service = service();

        assertThrows(NotFoundException.class, () -> service.create(notification));
        verify(alertBoard, never()).raise(any(), any());
    }

    @Test
    void markAllAsReadMarksTheWaterSupplyAlerts() {
        when(alertBoard.markAllRead(SourceModule.WATERSUPPLY)).thenReturn(List.of(alert(1L, 5L, true)));

        List<NotificationResponse> all = service().markAllAsRead();

        assertEquals(1, all.size());
        assertEquals(true, all.get(0).read());
    }

    @Test
    void createRaisesAWaterSupplyAlertForAPersonOfTheStaffReferential() {
        Long userId = personDirectory.idOf("Awa Demo");
        when(alertBoard.raise(eq(SourceModule.WATERSUPPLY), any())).thenReturn(alert(3L, userId, false));

        NotificationResponse created = service().create(
            new NotificationCreateRequest(userId, "Title", "Message", "warning", false, null));

        ArgumentCaptor<AlertDetails> details = ArgumentCaptor.forClass(AlertDetails.class);
        verify(alertBoard).raise(eq(SourceModule.WATERSUPPLY), details.capture());
        assertEquals(userId, details.getValue().recipientPersonId());
        assertEquals("warning", details.getValue().type());
        assertEquals(AlertSeverity.WARNING, details.getValue().severity());
        assertEquals(userId, created.userId());
        assertEquals(3L, created.id());
    }

    @Test
    void anUnknownOrForeignIdIsNotFound() {
        when(alertBoard.find(SourceModule.WATERSUPPLY, 9L)).thenReturn(Optional.empty());
        when(alertBoard.delete(SourceModule.WATERSUPPLY, 9L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service().get(9L));
        assertThrows(NotFoundException.class, () -> service().delete(9L));
    }

    private NotificationService service() {
        return new NotificationService(alertBoard, personDirectory);
    }

    private static AlertDto alert(Long id, Long recipient, boolean read) {
        Instant now = Instant.now();
        return new AlertDto(id, SourceModule.WATERSUPPLY, null, "warning", AlertSeverity.WARNING, AlertStatus.OPEN,
            "Title", "Message", null, null, null, null, recipient, read, null, now, null, now, now);
    }
}
