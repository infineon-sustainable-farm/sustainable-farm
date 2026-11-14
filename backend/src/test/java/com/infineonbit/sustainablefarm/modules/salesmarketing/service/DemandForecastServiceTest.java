package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.common.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.DemandForecast;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.DemandForecastRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandForecastServiceTest {

    @Mock
    private DemandForecastRepository repository;

    @InjectMocks
    private DemandForecastService service;

    @Test
    void getAllReturnsRepositoryResults() {
        List<DemandForecast> forecasts = List.of(new DemandForecast());
        when(repository.findAll()).thenReturn(forecasts);

        assertSame(forecasts, service.getAll());
    }

    @Test
    void getByIdThrowsWhenForecastDoesNotExist() {
        when(repository.findById(42)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class, () -> service.getById(42));

        assertEquals("DemandForecast not found with id: 42", exception.getMessage());
    }

    @Test
    void createPropagatesRepositoryFailure() {
        DemandForecast forecast = new DemandForecast();
        when(repository.save(forecast)).thenThrow(new IllegalStateException("repository failure"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> service.create(forecast));

        assertEquals("repository failure", exception.getMessage());
        verify(repository).save(forecast);
    }
}
