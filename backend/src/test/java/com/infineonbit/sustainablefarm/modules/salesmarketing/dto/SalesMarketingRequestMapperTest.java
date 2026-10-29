package com.infineonbit.sustainablefarm.modules.salesmarketing.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.CampaignRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Campaign;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SalesMarketingRequestMapperTest {

    private final SalesMarketingRequestMapper mapper = new SalesMarketingRequestMapper();

    @Test
    void mapsAllowedCampaignFieldsWithoutServerManagedValues() {
        CampaignRequest request = new CampaignRequest(
                "Harvest launch",
                "planned",
                new BigDecimal("1200.00"),
                BigDecimal.ZERO,
                5000,
                "EUR",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 11, 30));

        Campaign campaign = mapper.toEntity(request, Campaign.class);

        assertEquals("Harvest launch", campaign.getName());
        assertEquals(new BigDecimal("1200.00"), campaign.getBudgetEur());
        assertEquals(LocalDate.of(2026, 10, 1), campaign.getStartDate());
        assertNull(campaign.getCampaignId());
        assertNull(campaign.getCreatedAt());
    }

    @Test
    void ignoresClientSuppliedIdAndServerManagedFields() throws JsonProcessingException {
        CampaignRequest request = new ObjectMapper().findAndRegisterModules().readValue(
                """
                {
                  "campaignId": 999,
                  "createdAt": "2026-10-21T10:00:00",
                  "name": "Harvest launch"
                }
                """,
                CampaignRequest.class);

        Campaign campaign = mapper.toEntity(request, Campaign.class);

        assertEquals("Harvest launch", campaign.getName());
        assertNull(campaign.getCampaignId());
        assertNull(campaign.getCreatedAt());
    }
}
