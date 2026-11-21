package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.DemandForecastRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.DemandForecastResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.DemandForecast;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.DemandForecastRepository;
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
class DemandForecastServiceTest {

    @Mock
    private DemandForecastRepository repository;

    @Mock
    private SalesMarketingRequestMapper mapper;

    @InjectMocks
    private DemandForecastService service;

    @Test
    void getAllMapsEveryRowToItsResponse() {
        DemandForecast row = new DemandForecast();
        row.setId(7L);
        when(repository.findAll()).thenReturn(List.of(row));

        List<DemandForecastResponse> all = service.getAll();

        assertEquals(1, all.size());
        assertEquals(7L, all.get(0).forecastId());
    }

    @Test
    void getByIdThrowsWhenTheRowDoesNotExist() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> service.getById(42L));

        assertEquals("DemandForecast not found with id: 42", exception.getMessage());
    }

    @Test
    void createPropagatesRepositoryFailure() {
        DemandForecastRequest request = new DemandForecastRequest(null, LocalDate.of(2026, 3, 1), "base", BigDecimal.TEN, null, null, null, null, null);
        DemandForecast entity = new DemandForecast();
        when(mapper.apply(any(DemandForecastRequest.class), any(DemandForecast.class))).thenReturn(entity);
        when(repository.save(entity)).thenThrow(new IllegalStateException("repository failure"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.create(request));

        assertEquals("repository failure", exception.getMessage());
        verify(repository).save(entity);
    }
}
