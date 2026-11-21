package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityPrincipal;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecurityAuditAction;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecurityAuditEventEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.SiteSecurityAuditEventRepository;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records administrative changes (operators, credentials). It must run inside the transaction
 * of the change itself, so that a change is never saved without its audit row, and a change
 * that fails leaves no row behind.
 */
@Service
@RequiredArgsConstructor
public class SiteSecurityAuditService {

    static final String ADMIN = "ADMIN";

    private static final Logger log = LoggerFactory.getLogger(SiteSecurityAuditService.class);

    private final SiteSecurityAuditEventRepository repository;

    /** A change made by an operator holding the admin role. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(SiteSecurityPrincipal actor, SiteSecurityAuditAction action, String targetId, String detail) {
        if (actor == null || actor.operatorId() == null) {
            // Only resolved operators reach these endpoints; refuse rather than store an anonymous change.
            throw new IllegalStateException("An administrative change needs an operator.");
        }
        repository.save(SiteSecurityAuditEventEntity.builder()
                .occurredAt(Instant.now())
                .actorOperatorId(actor.operatorId())
                .actorKind(ADMIN)
                .actionName(action.name())
                .targetType(action.targetType())
                .targetId(targetId)
                .detail(detail == null || detail.length() <= 64 ? detail : detail.substring(0, 64))
                .build());
        log.info("Site Security audit: {} on {} {} by {}", action, action.targetType().toLowerCase(Locale.ROOT),
                targetId, actor.operatorId());
    }
}
