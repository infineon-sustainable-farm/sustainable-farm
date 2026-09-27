package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ForecastModelRun;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.ForecastModelRunService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forecast-model-runs")
@CrossOrigin(origins = "http://localhost:3000")
public class ForecastModelRunController {

    private final ForecastModelRunService service;

    public ForecastModelRunController(ForecastModelRunService service) {
        this.service = service;
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
    public ForecastModelRun create(@Valid @RequestBody ForecastModelRun item) {
        return service.create(item);
    }

    @PutMapping("/{id}")
    public ForecastModelRun update(
            @PathVariable Integer id,
            @Valid @RequestBody ForecastModelRun item) {
        return service.update(id, item);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
