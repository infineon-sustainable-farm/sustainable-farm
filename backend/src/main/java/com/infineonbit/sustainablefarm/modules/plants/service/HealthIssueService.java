package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HealthIssueRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthIssueResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthIssueReferenceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The catalogue of pests and diseases that inspections and treatments refer to.
 *
 * <p>Outside the dev profile no catalogue is loaded at startup: the user adds
 * each pest or disease with {@link #createIssue(HealthIssueRequest)}, and
 * "Other" is added the first time an inspection or a treatment uses it.
 */
@Service
public class HealthIssueService {

    /** {@code source} of an issue added without one. */
    static final String USER_ENTRY_SOURCE = "user_entry";

    /** {@code source} of "Other", the same as in the dev catalogue: it exists by definition. */
    static final String OTHER_SOURCE = "by_definition";

    /**
     * Pests first, then diseases, then "Other", each by name. Sorted in Java:
     * the kind is stored as text, which SQL would sort alphabetically.
     */
    private static final Comparator<HealthIssueReference> CATALOGUE_ORDER =
            Comparator.comparing(HealthIssueReference::getKind)
                    .thenComparing(HealthIssueReference::getName)
                    .thenComparing(HealthIssueReference::getId);

    private final HealthIssueReferenceRepository healthIssueReferenceRepository;

    /** Runs the insert of "Other" in a transaction of its own, committed at once. */
    private final TransactionTemplate ownTransaction;

    public HealthIssueService(HealthIssueReferenceRepository healthIssueReferenceRepository,
                              PlatformTransactionManager transactionManager) {
        this.healthIssueReferenceRepository = healthIssueReferenceRepository;
        TransactionTemplate newTransaction = new TransactionTemplate(transactionManager);
        newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.ownTransaction = newTransaction;
    }

    /**
     * An optional text as stored: trimmed, and {@code null} when it is blank.
     *
     * @param value the text as received, possibly {@code null}
     * @return the trimmed text, or {@code null} if it was null or blank
     */
    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** A code as stored: trimmed and upper-cased, so {@code " fruit_fly "} becomes {@code FRUIT_FLY}. */
    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private static String codeTaken(String code) {
        return "A health issue with code " + code + " already exists";
    }

    static HealthIssueResponse toResponse(HealthIssueReference issue) {
        return new HealthIssueResponse(
                issue.getId(),
                issue.getCode(),
                issue.getName(),
                issue.getKind(),
                issue.getScientificName(),
                issue.getEppoCode(),
                issue.getSource());
    }

    /**
     * Retrieves the whole catalogue: pests, then diseases, then "Other", each by
     * name, so "Other" stays last whatever is added later.
     *
     * @return the catalogue, possibly empty
     */
    public List<HealthIssueResponse> getAllIssues() {
        return healthIssueReferenceRepository.findAll().stream()
                .sorted(CATALOGUE_ORDER)
                .map(HealthIssueService::toResponse)
                .toList();
    }

    /**
     * Adds a pest or a disease to the catalogue.
     *
     * <p>Its code is derived from its name by
     * {@link HealthIssueReference#codeOf(String)}; the request validation
     * already refused a name that gives no code, or gives {@code OTHER}. A name
     * already in the catalogue is refused, ignoring case, accents and
     * surrounding spaces, as for a fertilizer. So is another name that gives a
     * code already taken, such as "Powdery-mildew" after "Powdery mildew". The
     * EPPO code is upper-cased, and a missing source is recorded as
     * {@code user_entry}.
     *
     * @param request the issue, already validated
     * @return the added issue
     * @throws ConflictException if the name or the code is already in the
     *                           catalogue, including when the same code was
     *                           being added at the same time
     */
    @Transactional
    public HealthIssueResponse createIssue(HealthIssueRequest request) {
        return createIssue(request, Instant.now());
    }

    /**
     * Same as {@link #createIssue(HealthIssueRequest)}, at an explicit write
     * time so the {@code lastUpdated} value can be tested.
     */
    HealthIssueResponse createIssue(HealthIssueRequest request, Instant now) {
        String name = request.name().trim();
        String code = HealthIssueReference.codeOf(name);
        String key = VarietyReferenceMatcher.matchKey(name);
        List<HealthIssueReference> catalogue = healthIssueReferenceRepository.findAll();
        catalogue.stream()
                .filter(existing -> VarietyReferenceMatcher.matchKey(existing.getName()).equals(key))
                .min(Comparator.comparing(HealthIssueReference::getId))
                .ifPresent(existing -> {
                    throw new ConflictException("A health issue named " + existing.getName() + " already exists");
                });
        if (catalogue.stream().anyMatch(existing -> existing.getCode().equals(code))) {
            throw new ConflictException(codeTaken(code));
        }

        String eppoCode = normalizeOptionalText(request.eppoCode());
        String source = normalizeOptionalText(request.source());
        HealthIssueReference issue = new HealthIssueReference(null, code, name,
                HealthIssueKind.valueOf(normalizeCode(request.kind())),
                normalizeOptionalText(request.scientificName()),
                eppoCode == null ? null : eppoCode.toUpperCase(Locale.ROOT),
                source == null ? USER_ENTRY_SOURCE : source,
                now);
        try {
            // Flushed here, so the unique constraint on the code answers inside this method.
            return toResponse(healthIssueReferenceRepository.saveAndFlush(issue));
        } catch (DataIntegrityViolationException sameCodeAtTheSameTime) {
            // The constraint answers once the other insert is committed: the code is taken by now.
            throw new ConflictException(codeTaken(code));
        }
    }

    /**
     * The catalogue rows of the codes sent by an inspection, in the same order,
     * compared without case or surrounding spaces: {@code " fruit_fly "} finds
     * {@code FRUIT_FLY}.
     *
     * <p>Every code is looked up before anything is written, so a code missing
     * from the catalogue refuses the request and adds nothing. {@code OTHER} is
     * the exception: when no row has it yet, it is added once every other code
     * is found, with the values of the dev catalogue (name "Other", kind
     * {@code OTHER}, source {@code by_definition}). The insert has a
     * transaction of its own, so the row stays even if the request fails
     * afterwards: it is the row that any later use would add. When two requests
     * add it at the same time, the unique code keeps one row, and both use it.
     *
     * @param codes the codes as received, already checked by the request pattern
     * @return the catalogue row of each code, in the order of the codes
     * @throws BusinessRuleException if a code other than {@code OTHER} is not in
     *                               the catalogue, answered with a 422: the code
     *                               has the right form, the catalogue refuses it
     */
    public List<HealthIssueReference> getIssuesByCode(List<String> codes) {
        return getIssuesByCode(codes, Instant.now());
    }

    /**
     * Same as {@link #getIssuesByCode(List)}, at an explicit write time so the
     * {@code lastUpdated} value of "Other" can be tested.
     */
    List<HealthIssueReference> getIssuesByCode(List<String> codes, Instant now) {
        List<String> normalizedCodes = codes.stream().map(HealthIssueService::normalizeCode).toList();
        Map<String, HealthIssueReference> issuesByCode = new HashMap<>();
        for (String code : new LinkedHashSet<>(normalizedCodes)) {
            Optional<HealthIssueReference> issue = healthIssueReferenceRepository.findByCode(code);
            if (issue.isPresent()) {
                issuesByCode.put(code, issue.get());
            } else if (!code.equals(HealthIssueReference.OTHER_CODE)) {
                throw new BusinessRuleException("No health issue with code " + code + " in the catalogue");
            }
        }
        if (normalizedCodes.contains(HealthIssueReference.OTHER_CODE)
                && !issuesByCode.containsKey(HealthIssueReference.OTHER_CODE)) {
            issuesByCode.put(HealthIssueReference.OTHER_CODE, addOther(now));
        }
        return normalizedCodes.stream().map(issuesByCode::get).toList();
    }

    /**
     * The catalogue row of one code, as {@link #getIssuesByCode(List)} finds it.
     *
     * @param code the code as received, already checked by the request pattern
     * @return the catalogue row
     * @throws BusinessRuleException if the code is not {@code OTHER} and is not
     *                               in the catalogue, answered with a 422
     */
    public HealthIssueReference getIssueByCode(String code) {
        return getIssuesByCode(List.of(code)).getFirst();
    }

    /**
     * Adds "Other" and returns it, as the current transaction reads it.
     *
     * <p>The insert is committed at once, in its own transaction. A request that
     * adds the row at the same time sees its own insert wait for this one, then
     * break the unique code: it reads the row committed here instead.
     */
    private HealthIssueReference addOther(Instant now) {
        try {
            ownTransaction.executeWithoutResult(status -> healthIssueReferenceRepository.saveAndFlush(
                    new HealthIssueReference(null, HealthIssueReference.OTHER_CODE, "Other", HealthIssueKind.OTHER,
                            null, null, OTHER_SOURCE, now)));
        } catch (DataIntegrityViolationException addedAtTheSameTime) {
            // Another request added the row first and has committed it.
            return healthIssueReferenceRepository.findByCode(HealthIssueReference.OTHER_CODE)
                    .orElseThrow(() -> addedAtTheSameTime);
        }
        return healthIssueReferenceRepository.findByCode(HealthIssueReference.OTHER_CODE).orElseThrow();
    }
}
