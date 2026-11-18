package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.infineonbit.sustainablefarm.modules.visitormanagement.api.VisitCancelled;
import com.infineonbit.sustainablefarm.modules.visitormanagement.api.VisitRescheduled;
import com.infineonbit.sustainablefarm.modules.visitormanagement.api.VisitScheduled;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.VisitorEventRequest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Visitor Management events land on the same path as {@code POST /api/water-supply/visitor-events}. */
@ExtendWith(MockitoExtension.class)
class VisitorManagementListenerTest {
    private static final Instant NINE = Instant.parse("2030-03-05T09:00:00Z");

    @Mock
    private VisitorIntegrationService visitorService;

    @Test
    void aScheduledVisitIsAnnouncedWithoutZones() {
        new VisitorManagementListener(visitorService).onVisitScheduled(
                new VisitScheduled("registration-7", "School A", NINE, 12));

        verify(visitorService).visitorEvent(new VisitorEventRequest("registration-7", "School A", NINE, List.of()));
    }

    @Test
    void aRescheduledVisitIsAnnouncedAgainUnderTheSameVisitId() {
        Instant two = Instant.parse("2030-03-05T14:00:00Z");

        new VisitorManagementListener(visitorService).onVisitRescheduled(
                new VisitRescheduled("booking-3", "Lucas Weber", two, 2));

        verify(visitorService).visitorEvent(new VisitorEventRequest("booking-3", "Lucas Weber", two, List.of()));
    }

    @Test
    void aCancelledVisitIsWithdrawn() {
        new VisitorManagementListener(visitorService).onVisitCancelled(new VisitCancelled("booking-3"));

        verify(visitorService).withdrawVisit("booking-3");
        verifyNoMoreInteractions(visitorService);
    }
}
