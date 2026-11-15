package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ForecastModelRun;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.ForecastModelRunService;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ForecastModelRunRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forecast-model-runs")
public class ForecastModelRunController {

    private final ForecastModelRunService service;
    private final SalesMarketingRequestMapper requestMapper;

    public ForecastModelRunController(ForecastModelRunService service, SalesMarketingRequestMapper requestMapper) {
        this.service = service;
        this.requestMapper = requestMapper;
    }

    @GetMapping
    public List<ForecastModelRun> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ForecastModelRun getOne(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ForecastModelRun create(@Valid @RequestBody ForecastModelRunRequest request) {
        return service.create(requestMapper.toEntity(request, ForecastModelRun.class));
    }

    @PutMapping("/{id}")
    public ForecastModelRun update(
            @PathVariable Integer id,
            @Valid @RequestBody ForecastModelRunRequest request) {
        return service.update(id, requestMapper.toEntity(request, ForecastModelRun.class));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
