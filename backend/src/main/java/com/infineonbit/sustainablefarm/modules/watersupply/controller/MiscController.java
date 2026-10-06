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
 * Sante technique du module, consommee par le badge "Backend connecte" du dashboard.
 *
 * <p>L'endpoint verifie reellement la base de donnees (une connexion est ouverte et validee)
 * au lieu de renvoyer une reponse statique : une base coupee faisait auparavant afficher un
 * backend "connecte". Contrat conserve pour les clients existants : 200 avec {@code status=ok}
 * quand tout va bien, 503 avec {@code status=degraded} sinon.</p>
 */
@RestController
public class MiscController {

    /** Delai de validation d'une connexion a la base (secondes). */
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
