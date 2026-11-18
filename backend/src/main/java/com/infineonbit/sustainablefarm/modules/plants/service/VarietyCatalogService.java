package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.api.CatalogVariety;
import com.infineonbit.sustainablefarm.modules.plants.api.VarietyCatalog;
import com.infineonbit.sustainablefarm.modules.plants.entity.VarietyReference;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The variety catalogue of {@link VarietyCatalog}, backed by the agronomic
 * reference ({@code variety_reference}). The table holds a handful of rows,
 * read whole and matched in Java, as in {@link VarietyReferenceService}.
 */
@Service
@Transactional(readOnly = true)
public class VarietyCatalogService implements VarietyCatalog {

    private static final Comparator<VarietyReference> BY_NAME = Comparator
            .comparing((VarietyReference reference) -> VarietyReferenceMatcher.matchKey(reference.getVarietyName()))
            .thenComparing(VarietyReference::getId);

    private final VarietyReferenceRepository varietyReferenceRepository;

    public VarietyCatalogService(VarietyReferenceRepository varietyReferenceRepository) {
        this.varietyReferenceRepository = varietyReferenceRepository;
    }

    @Override
    public List<CatalogVariety> listVarieties() {
        return varietyReferenceRepository.findAll().stream()
                .sorted(BY_NAME)
                .map(VarietyCatalogService::toCatalogVariety)
                .toList();
    }

    @Override
    public Optional<CatalogVariety> findByKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        return varietyReferenceRepository.findByVarietyKey(key).map(VarietyCatalogService::toCatalogVariety);
    }

    @Override
    public Optional<CatalogVariety> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return VarietyReferenceMatcher.find(name, varietyReferenceRepository.findAll())
                .map(VarietyCatalogService::toCatalogVariety);
    }

    @Override
    public Optional<CatalogVariety> resolve(String freeName) {
        if (freeName == null || freeName.isBlank()) {
            return Optional.empty();
        }
        String wanted = VarietyReference.keyOf(freeName);
        List<VarietyReference> references = varietyReferenceRepository.findAll();
        return references.stream()
                .filter(reference -> reference.getVarietyKey().equals(wanted))
                .findFirst()
                .or(() -> references.stream()
                        .filter(reference -> VarietyReference.keyOf(reference.getVarietyName()).equals(wanted))
                        .min(Comparator.comparing(VarietyReference::getId)))
                .map(VarietyCatalogService::toCatalogVariety);
    }

    private static CatalogVariety toCatalogVariety(VarietyReference reference) {
        return new CatalogVariety(reference.getVarietyKey(), reference.getVarietyName());
    }
}
