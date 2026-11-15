package com.infineonbit.sustainablefarm.modules.watersupply.iot;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Entry point for IoT sensors - real system integration. See IotTelemetryService for the routing. */
@RestController
@RequestMapping("/api/iot")
public class IotTelemetryController {

    private final IotTelemetryService telemetryService;

    public IotTelemetryController(IotTelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    /** Accepts a single object or a batch of measurements. Response 202 with the status of each measurement. */
    @PostMapping("/telemetry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public List<IotTelemetryService.Result> ingest(@RequestBody Object body) {
        List<IotTelemetryService.Telemetry> batch = new ArrayList<>();
        if (body instanceof List<?> list) {
            for (Object item : list) {
                batch.add(parseSafe(item));
            }
        } else {
            batch.add(parseSafe(body));
        }
        return telemetryService.ingest(batch);
    }

    /** An unreadable measurement never fails the batch: it is rejected with the reason. */
    private IotTelemetryService.Telemetry parseSafe(Object item) {
        try {
            return telemetryService.parse(item);
        } catch (Exception e) {
            return telemetryService.errorTelemetry(e);
        }
    }
}
