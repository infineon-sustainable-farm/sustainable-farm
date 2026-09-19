package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterQuotaCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterQuotaResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterQuotaUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.service.WaterQuotaService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quotas mensuels d'eau par ferme ou par zone (P8).
 *
 * <p>CRUD complet + suivi de consommation du mois courant avec alertes automatiques
 * {@code warning} a 80&nbsp;% et {@code critical} a 100&nbsp;% du quota.</p>
 */
@RestController
@RequestMapping("/api/water/quotas")
public class WaterQuotaController {

    private final WaterQuotaService quotaService;

    public WaterQuotaController(WaterQuotaService quotaService) {
        this.quotaService = quotaService;
    }

    /** Liste des quotas, filtrable par cible (targetType=farm|zone, targetId). */
    @GetMapping
    public List<WaterQuotaResponse> quotas(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) UUID targetId) {
        if (targetType != null && targetId != null) {
            return quotaService.findQuotas(targetType, targetId);
        }
        return quotaService.findQuotas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WaterQuotaResponse createQuota(@Valid @RequestBody WaterQuotaCreateRequest request) {
        return quotaService.createQuota(request);
    }

    @GetMapping("/{quotaId}")
    public WaterQuotaResponse quota(@PathVariable UUID quotaId) {
        return quotaService.getQuota(quotaId);
    }

    @PutMapping("/{quotaId}")
    public WaterQuotaResponse updateQuota(@PathVariable UUID quotaId,
                                          @RequestBody WaterQuotaUpdateRequest request) {
        return quotaService.updateQuota(quotaId, request);
    }

    @DeleteMapping("/{quotaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuota(@PathVariable UUID quotaId) {
        quotaService.deleteQuota(quotaId);
    }

    /**
     * Suivi du mois courant : pour chaque quota, la consommation cumulee des capteurs,
     * le pourcentage utilise et le statut (ok, warning a 80 %, exceeded a 100 %).
     */
    @GetMapping("/usage")
    public List<Map<String, Object>> usage(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        return month == null ? quotaService.currentUsage() : quotaService.usageForMonth(month);
    }
}
