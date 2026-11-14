package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityPrincipal;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecurityAuditAction;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecurityAuditEventEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.SiteSecurityAuditEventRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records administrative and account changes. It must run inside the transaction
 * of the change itself, so that a change is never saved without its audit row,
 * and a change that fails leaves no row behind.
 */
@Service
@RequiredArgsConstructor
public class SiteSecurityAuditService {

    public static final String SYSTEM_ACTOR = "system";
    static final String ADMIN = "ADMIN";
    static final String SELF = "SELF";
    static final String SYSTEM = "SYSTEM";

    private static final Logger log = LoggerFactory.getLogger(SiteSecurityAuditService.class);

    private final SiteSecurityAuditEventRepository repository;

    /** A change made by a signed-in operator (an admin action). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(SiteSecurityPrincipal actor, SiteSecurityAuditAction action, String targetId, String detail) {
        if (actor == null || actor.operatorId() == null) {
            // Only signed-in operators reach these endpoints; refuse rather than store an anonymous change.
            throw new IllegalStateException("An administrative change needs a signed-in operator.");
        }
        save(actor.operatorId(), ADMIN, action, targetId, detail);
    }

    /** A change the account made to itself, for example activating it or resetting its password. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordSelf(String accountId, SiteSecurityAuditAction action, String detail) {
        if (accountId == null) {
            throw new IllegalStateException("A self-service event needs the account id.");
        }
        save(accountId, SELF, action, accountId, detail);
    }

    /** A change made by the module itself, for example blocking sign-in after repeated wrong passwords. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordSystem(SiteSecurityAuditAction action, String targetId, String detail) {
        save(SYSTEM_ACTOR, SYSTEM, action, targetId, detail);
    }

    private void save(String actorId, String actorKind, SiteSecurityAuditAction action, String targetId, String detail) {
        repository.save(SiteSecurityAuditEventEntity.builder()
                .id(UUID.randomUUID().toString())
                .occurredAt(Instant.now())
                .actorOperatorId(actorId)
                .actorKind(actorKind)
                .actionName(action.name())
                .targetType(action.targetType())
                .targetId(targetId)
                .detail(detail == null || detail.length() <= 64 ? detail : detail.substring(0, 64))
                .build());
        log.info("Site Security audit: {} on {} {} by {} {}", action, action.targetType().toLowerCase(Locale.ROOT),
                targetId, actorKind.toLowerCase(Locale.ROOT), actorId);
    }
}
