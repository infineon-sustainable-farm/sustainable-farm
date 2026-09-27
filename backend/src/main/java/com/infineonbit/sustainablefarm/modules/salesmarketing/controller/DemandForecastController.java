package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.DemandForecast;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.DemandForecastService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/demand-forecasts")
@CrossOrigin(origins = "http://localhost:3000")
public class DemandForecastController {

    private final DemandForecastService service;

    public DemandForecastController(DemandForecastService service) {
        this.service = service;
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
    public DemandForecast create(@Valid @RequestBody DemandForecast item) {
        return service.create(item);
    }

    @PutMapping("/{id}")
    public DemandForecast update(
            @PathVariable Integer id,
            @Valid @RequestBody DemandForecast item) {
        return service.update(id, item);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
