package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.NotificationCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.NotificationResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.service.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Object list(
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) Integer size,
            @RequestParam(required = false) UUID userId) {
        if (page == null && size == null && userId == null) return notificationService.findAll();
        return notificationService.findAll(pageRequest(page, size), userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse create(@Valid @RequestBody NotificationCreateRequest request) {
        return notificationService.create(request);
    }

    @GetMapping("/{notificationId}")
    public NotificationResponse get(@PathVariable UUID notificationId) {
        return notificationService.get(notificationId);
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(@PathVariable UUID notificationId) {
        return notificationService.markAsRead(notificationId);
    }

    @PatchMapping("/read-all")
    public List<NotificationResponse> markAllAsRead() {
        return notificationService.markAllAsRead();
    }

    @DeleteMapping("/{notificationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID notificationId) {
        notificationService.delete(notificationId);
    }

    private PageRequest pageRequest(Integer page, Integer size) {
        return PageRequest.of(page == null ? 0 : page, size == null ? 20 : size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
