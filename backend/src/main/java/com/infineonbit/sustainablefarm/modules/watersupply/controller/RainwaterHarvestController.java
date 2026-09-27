package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.PageResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.RainwaterHarvestRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.RainwaterHarvestResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.service.RainwaterHarvestService;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
 * Collecte d'eau de pluie (module 5.1).
 *
 * <p>L'API expose des DTO et non l'entite JPA : le client ne peut donc pas ecrire un identifiant
 * ou une date de creation. La liste repond soit en tableau simple (comportement historique), soit
 * en page des que {@code page} ou {@code size} est fourni — meme convention que les autres listes
 * du module.</p>
 */
@RestController
@RequestMapping("/api/rainwater-harvests")
public class RainwaterHarvestController {
    private final RainwaterHarvestService rainwaterHarvestService;

    public RainwaterHarvestController(RainwaterHarvestService rainwaterHarvestService) {
        this.rainwaterHarvestService = rainwaterHarvestService;
    }

    @GetMapping
    public Object list(
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) Integer size,
            @RequestParam(required = false) UUID sourceId) {
        if (page == null && size == null && sourceId == null) {
            return rainwaterHarvestService.findAll();
        }
        return rainwaterHarvestService.findAll(
                PageRequest.of(page == null ? 0 : page, size == null ? 20 : size,
                        Sort.by(Sort.Direction.ASC, "createdAt")),
                sourceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RainwaterHarvestResponse create(@RequestBody RainwaterHarvestRequest request) {
        return rainwaterHarvestService.create(request);
    }

    @GetMapping("/{harvestId}")
    public RainwaterHarvestResponse get(@PathVariable UUID harvestId) {
        return rainwaterHarvestService.get(harvestId);
    }

    @PutMapping("/{harvestId}")
    public RainwaterHarvestResponse update(@PathVariable UUID harvestId,
            @RequestBody RainwaterHarvestRequest request) {
        return rainwaterHarvestService.update(harvestId, request);
    }

    @DeleteMapping("/{harvestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID harvestId) {
        rainwaterHarvestService.delete(harvestId);
    }

    @GetMapping("/coverage")
    public Map<String, Object> coverage(@RequestParam(defaultValue = "month") String period) {
        return rainwaterHarvestService.coverage(period);
    }
}
