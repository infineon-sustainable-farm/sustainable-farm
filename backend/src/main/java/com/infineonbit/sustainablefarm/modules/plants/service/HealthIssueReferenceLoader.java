package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthIssueReferenceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Loads a default catalogue of health issues at startup in the dev profile: the
 * pests and diseases of the mango documented in Burkina Faso, and "Other" for
 * anything else.
 *
 * <p>Runs in the dev profile only, like {@link CurrencyRateLoader}: no
 * reference data is loaded in production. There, the user adds the pests and
 * diseases with {@code POST /api/plants/health-issues}, and the service adds
 * "Other" the first time an inspection or a treatment uses it, with the values
 * below.
 *
 * <p>A row is inserted only when its code is missing. An existing row is never
 * overwritten, even when it differs from the default below: a row corrected in
 * the database is the one that counts. Restarting the application therefore
 * never duplicates nor resets a row.
 */
@Component
@Profile("dev")
public class HealthIssueReferenceLoader implements CommandLineRunner {

    /** Default catalogue row, with its source. */
    private record IssueDefault(String code, String name, HealthIssueKind kind, String scientificName,
                                String eppoCode, String source) {
    }

    private static final List<IssueDefault> ISSUE_DEFAULTS = List.of(
            new IssueDefault("FRUIT_FLY", "Fruit flies", HealthIssueKind.PEST,
                    "Tephritidae, mainly Bactrocera dorsalis and Ceratitis cosyra", null, "Zida_2023"),
            new IssueDefault("MANGO_MEALYBUG", "Mango mealybug", HealthIssueKind.PEST,
                    "Rastrococcus invadens", "RASTIN", "Vayssieres_2012"),
            new IssueDefault("TERMITES", "Termites", HealthIssueKind.PEST, null, null, "Vayssieres_2012"),
            new IssueDefault("SCALE_INSECTS", "Scale insects", HealthIssueKind.PEST, null, null, "Vayssieres_2012"),
            new IssueDefault("ANTHRACNOSE", "Anthracnose", HealthIssueKind.DISEASE,
                    "Colletotrichum gloeosporioides", "COLLGL", "Dianda_2025"),
            new IssueDefault("BACTERIAL_BLACK_SPOT", "Bacterial black spot", HealthIssueKind.DISEASE,
                    "Xanthomonas citri pv. mangiferaeindicae", "XANTMI", "Dianda_2025"),
            new IssueDefault("MANGO_DECLINE", "Mango decline", HealthIssueKind.DISEASE, null, null, "Dianda_2025"),
            new IssueDefault("OTHER", "Other", HealthIssueKind.OTHER, null, null, "by_definition"));

    private final HealthIssueReferenceRepository healthIssueReferenceRepository;

    public HealthIssueReferenceLoader(HealthIssueReferenceRepository healthIssueReferenceRepository) {
        this.healthIssueReferenceRepository = healthIssueReferenceRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        load(Instant.now());
    }

    /**
     * Inserts the missing catalogue rows, at an explicit write time so the
     * {@code lastUpdated} values can be tested.
     *
     * @param now insertion time, stored as {@code lastUpdated} of each inserted row
     */
    void load(Instant now) {
        for (IssueDefault issue : ISSUE_DEFAULTS) {
            if (healthIssueReferenceRepository.findByCode(issue.code()).isEmpty()) {
                healthIssueReferenceRepository.save(new HealthIssueReference(null, issue.code(), issue.name(),
                        issue.kind(), issue.scientificName(), issue.eppoCode(), issue.source(), now));
            }
        }
    }
}
