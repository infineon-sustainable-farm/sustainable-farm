package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.PricingHistory;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.PricingHistoryRepository;
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
class PricingHistoryServiceTest {

    @Mock
    private PricingHistoryRepository repository;

    @InjectMocks
    private PricingHistoryService service;

    @Test
    void getAllReturnsRepositoryResults() {
        List<PricingHistory> history = List.of(new PricingHistory());
        when(repository.findAll()).thenReturn(history);

        assertSame(history, service.getAll());
    }

    @Test
    void getByIdThrowsWhenPricingHistoryDoesNotExist() {
        when(repository.findById(42)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.getById(42));

        assertEquals("PricingHistory not found with id: 42", exception.getMessage());
    }

    @Test
    void createPropagatesRepositoryFailure() {
        PricingHistory history = new PricingHistory();
        when(repository.save(history)).thenThrow(new IllegalStateException("repository failure"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> service.create(history));

        assertEquals("repository failure", exception.getMessage());
        verify(repository).save(history);
    }
}
