package com.infineonbit.sustainablefarm.modules.watersupply.iot;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Point d entree des capteurs IoT - integration systeme reel. Voir IotTelemetryService pour le routage. */
@RestController
@RequestMapping("/api/iot")
public class IotTelemetryController {

    private final IotTelemetryService telemetryService;

    public IotTelemetryController(IotTelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    /** Accepte un objet unique ou un lot de mesures. Reponse 202 avec le statut de chaque mesure. */
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

    /** Une mesure illisible ne fait jamais echouer le lot : elle est rejetee avec le motif. */
    private IotTelemetryService.Telemetry parseSafe(Object item) {
        try {
            return telemetryService.parse(item);
        } catch (Exception e) {
            return telemetryService.errorTelemetry(e);
        }
    }
}
