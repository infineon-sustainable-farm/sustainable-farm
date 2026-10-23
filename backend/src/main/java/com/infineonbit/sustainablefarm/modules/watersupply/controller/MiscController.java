package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Technical health of the module, consumed by the dashboard's "Backend connected" badge.
 *
 * <p>The endpoint really checks the database (a connection is opened and validated)
 * instead of returning a static response: a disconnected database used to still display a
 * "connected" backend. Contract preserved for existing clients: 200 with {@code status=ok}
 * when everything is fine, 503 with {@code status=degraded} otherwise.</p>
 */
@RestController
public class MiscController {

    /** Validation timeout for a database connection (seconds). */
    private static final int VALIDATION_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;

    public MiscController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean databaseUp = databaseIsUp();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", databaseUp ? "ok" : "degraded");
        body.put("database", databaseUp ? "up" : "down");
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity
                .status(databaseUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(body);
    }

    private boolean databaseIsUp() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(VALIDATION_TIMEOUT_SECONDS);
        } catch (SQLException ex) {
            return false;
        }
    }
}
