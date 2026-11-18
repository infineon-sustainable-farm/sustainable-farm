package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ForecastModelRunRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.ForecastModelRunResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.ForecastModelRunService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/forecast-model-runs")
public class ForecastModelRunController {

    private final ForecastModelRunService service;

    public ForecastModelRunController(ForecastModelRunService service) {
        this.service = service;
    }

    @GetMapping
    public List<ForecastModelRunResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ForecastModelRunResponse getOne(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ForecastModelRunResponse create(@Valid @RequestBody ForecastModelRunRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ForecastModelRunResponse update(@PathVariable Long id, @Valid @RequestBody ForecastModelRunRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
