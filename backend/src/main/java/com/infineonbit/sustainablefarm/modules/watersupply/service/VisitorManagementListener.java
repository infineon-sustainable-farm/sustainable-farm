package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.visitormanagement.api.VisitCancelled;
import com.infineonbit.sustainablefarm.modules.visitormanagement.api.VisitRescheduled;
import com.infineonbit.sustainablefarm.modules.visitormanagement.api.VisitScheduled;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.VisitorEventRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Keeps the visit announcements ({@code WaterVisitorEvent}) in step with Visitor Management.
 *
 * <p>Runs once the visit is committed on the Visitor Management side, in its own transaction, and
 * goes through the same path as {@code POST /api/water/visitor-events}: idempotent on
 * {@code visitId}, notification for the water team. Visitor Management knows no farm zone, so the
 * announcement carries none and no zone conflict can be flagged from it. The source transaction is
 * already committed: a failure here is logged by Spring and never undoes the booking or the
 * registration.</p>
 */
@Component
public class VisitorManagementListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(VisitorManagementListener.class);

    private final VisitorIntegrationService visitorService;

    public VisitorManagementListener(VisitorIntegrationService visitorService) {
        this.visitorService = visitorService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onVisitScheduled(VisitScheduled event) {
        announce(event.visitId(), event.groupName(), event.expectedAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onVisitRescheduled(VisitRescheduled event) {
        announce(event.visitId(), event.groupName(), event.expectedAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onVisitCancelled(VisitCancelled event) {
        if (visitorService.withdrawVisit(event.visitId())) {
            LOGGER.info("Visit {} cancelled in Visitor Management: announcement withdrawn", event.visitId());
        }
    }

    private void announce(String visitId, String groupName, Instant expectedAt) {
        visitorService.visitorEvent(new VisitorEventRequest(visitId, groupName, expectedAt, List.of()));
    }
}
