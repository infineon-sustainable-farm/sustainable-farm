package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.core.web.PageResponse;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertBoard;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertDetails;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertDto;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.NotificationCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.NotificationResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The notification center of Water Supply: the module's alerts in the single alert store of the
 * notification module (ADR 0004), under the former {@code /api/notifications} JSON, now served at {@code /api/water-supply/notifications}.
 */
@Service
@Transactional(readOnly = true)
public class NotificationService {
    private static final String NOTIFICATION = "Notification";

    private final AlertBoard alertBoard;
    private final PersonDirectory personDirectory;

    public NotificationService(AlertBoard alertBoard, PersonDirectory personDirectory) {
        this.alertBoard = alertBoard;
        this.personDirectory = personDirectory;
    }

    public List<NotificationResponse> findAll() {
        return alertBoard.list(AlertService.SOURCE, null).stream().map(NotificationResponse::from).toList();
    }

    /** A page of the notifications; the sort attributes are those of the alert ({@code raisedAt}). */
    public PageResponse<NotificationResponse> findAll(Pageable pageable, Long userId) {
        Page<AlertDto> page = alertBoard.page(AlertService.SOURCE, userId, pageable);
        return new PageResponse<>(page.map(NotificationResponse::from).getContent(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public NotificationResponse create(NotificationCreateRequest request) {
        requireUser(request.userId());
        return NotificationResponse.from(alertBoard.raise(AlertService.SOURCE, AlertDetails.builder()
                .recipientPersonId(request.userId())
                .title(request.title())
                .message(request.message())
                .type(request.type())
                .severity(AlertService.severityOf(request.type()))
                .read(request.read() != null && request.read())
                .actionUrl(request.actionUrl())
                .build()));
    }

    public NotificationResponse get(Long notificationId) {
        return NotificationResponse.from(alertBoard.find(AlertService.SOURCE, notificationId)
                .orElseThrow(() -> new NotFoundException(NOTIFICATION)));
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        return NotificationResponse.from(alertBoard.markRead(AlertService.SOURCE, notificationId)
                .orElseThrow(() -> new NotFoundException(NOTIFICATION)));
    }

    @Transactional
    public List<NotificationResponse> markAllAsRead() {
        return alertBoard.markAllRead(AlertService.SOURCE).stream().map(NotificationResponse::from).toList();
    }

    @Transactional
    public void delete(Long notificationId) {
        if (!alertBoard.delete(AlertService.SOURCE, notificationId)) {
            throw new NotFoundException(NOTIFICATION);
        }
    }

    /** The recipient is a person of the staff referential (docs/adr/0004). */
    private void requireUser(Long userId) {
        if (!personDirectory.exists(userId)) {
            throw new NotFoundException("User");
        }
    }
}
