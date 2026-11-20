package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.PricingHistoryRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.PricingHistoryResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.PricingHistory;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.PricingHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingHistoryServiceTest {

    @Mock
    private PricingHistoryRepository repository;

    @Mock
    private SalesMarketingRequestMapper mapper;

    @InjectMocks
    private PricingHistoryService service;

    @Test
    void getAllMapsEveryRowToItsResponse() {
        PricingHistory row = new PricingHistory();
        row.setId(7L);
        when(repository.findAll()).thenReturn(List.of(row));

        List<PricingHistoryResponse> all = service.getAll();

        assertEquals(1, all.size());
        assertEquals(7L, all.get(0).pricingId());
    }

    @Test
    void getByIdThrowsWhenTheRowDoesNotExist() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.getById(42L));

        assertEquals("PricingHistory not found with id: 42", exception.getMessage());
    }

    @Test
    void createPropagatesRepositoryFailure() {
        PricingHistoryRequest request = new PricingHistoryRequest(1L, LocalDate.of(2026, 2, 1), new BigDecimal("7.25"), null, null, null, null, null,
                null, null);
        PricingHistory entity = new PricingHistory();
        when(mapper.apply(any(PricingHistoryRequest.class), any(PricingHistory.class))).thenReturn(entity);
        when(repository.save(entity)).thenThrow(new IllegalStateException("repository failure"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.create(request));

        assertEquals("repository failure", exception.getMessage());
        verify(repository).save(entity);
    }
}
