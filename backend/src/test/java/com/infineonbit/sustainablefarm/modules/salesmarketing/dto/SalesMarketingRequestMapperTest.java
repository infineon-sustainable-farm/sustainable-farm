package com.infineonbit.sustainablefarm.modules.salesmarketing.dto;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.money.CurrencyCode;
import com.infineonbit.sustainablefarm.core.money.Money;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.CampaignRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.OrderRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ShipmentRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Campaign;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CampaignStatus;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Customer;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Order;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Shipment;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ShipmentStatus;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.CampaignRepository;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.CustomerRepository;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.DemandReportRepository;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.OrderRepository;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.ProductRepository;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.SalesChannelRepository;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SalesMarketingRequestMapperTest {

    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final SalesChannelRepository channels = mock(SalesChannelRepository.class);
    private SalesMarketingRequestMapper mapper;

    @BeforeEach
    void mapper() {
        mapper = new SalesMarketingRequestMapper(mock(CampaignRepository.class), customers,
                mock(DemandReportRepository.class), mock(OrderRepository.class), mock(ProductRepository.class),
                channels, mock(ShipmentRepository.class));
    }

    @Test
    void writesTheRequestFieldsWithEuroAmountsAndLeavesTheIdAndAuditTimes() {
        CampaignRequest request = new CampaignRequest("Harvest launch", "active", new BigDecimal("1200"),
                BigDecimal.ZERO, 5000, "EUR", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 30));

        Campaign campaign = mapper.apply(request, new Campaign());

        assertEquals("Harvest launch", campaign.getName());
        assertEquals(CampaignStatus.ACTIVE, campaign.getStatus());
        assertEquals(Money.of("1200.00", CurrencyCode.EUR), campaign.getBudget());
        assertEquals(LocalDate.of(2026, 10, 1), campaign.getStartDate());
        assertNull(campaign.getId());
        assertNull(campaign.getCreatedAt());
    }

    @Test
    void aMissingStatusTakesTheDefaultOfItsColumn() {
        Campaign campaign = mapper.apply(new CampaignRequest("Minimal", null, null, null, null, null, null, null),
                new Campaign());
        Shipment shipment = mapper.apply(new ShipmentRequest(null, null, "A", "B", null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null), new Shipment());

        assertEquals(CampaignStatus.SCHEDULED, campaign.getStatus());
        assertNull(campaign.getBudget());
        assertEquals(ShipmentStatus.PACKED, shipment.getStatus());
    }

    @Test
    void aStatusOutsideItsCheckConstraintIsAConflict() {
        CampaignRequest request = new CampaignRequest("Harvest launch", "planned", null, null, null, null, null, null);

        assertThrows(ConflictException.class, () -> mapper.apply(request, new Campaign()));
    }

    @Test
    void referencedRowsAreSetByReference() {
        Customer customer = new Customer();
        when(customers.getReferenceById(3L)).thenReturn(customer);

        Order order = mapper.apply(new OrderRequest(3L, null, LocalDate.of(2026, 2, 10), null,
                new BigDecimal("8400"), "pending"), new Order());

        assertSame(customer, order.getCustomer());
        assertNull(order.getSalesChannel());
        assertEquals(new BigDecimal("8400.00"), order.getTotalValue().amount());
        verifyNoInteractions(channels);
    }
}
