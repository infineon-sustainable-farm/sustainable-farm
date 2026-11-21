
package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.AlertRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.AlertResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales-marketing/alerts")
public class AlertController {

    private final AlertService service;

    public AlertController(AlertService service) {
        this.service = service;
    }

    @GetMapping
    public List<AlertResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public AlertResponse getOne(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlertResponse create(@Valid @RequestBody AlertRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public AlertResponse update(
            @PathVariable Long id,
            @Valid @RequestBody AlertRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}