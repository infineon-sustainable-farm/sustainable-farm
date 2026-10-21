package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.DemandForecast;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.DemandForecastService;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.DemandForecastRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/demand-forecasts")
public class DemandForecastController {

    private final DemandForecastService service;
    private final SalesMarketingRequestMapper requestMapper;

    public DemandForecastController(DemandForecastService service, SalesMarketingRequestMapper requestMapper) {
        this.service = service;
        this.requestMapper = requestMapper;
    }

    @GetMapping
    public List<DemandForecast> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public DemandForecast getOne(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DemandForecast create(@Valid @RequestBody DemandForecastRequest request) {
        return service.create(requestMapper.toEntity(request, DemandForecast.class));
    }

    @PutMapping("/{id}")
    public DemandForecast update(
            @PathVariable Integer id,
            @Valid @RequestBody DemandForecastRequest request) {
        return service.update(id, requestMapper.toEntity(request, DemandForecast.class));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
