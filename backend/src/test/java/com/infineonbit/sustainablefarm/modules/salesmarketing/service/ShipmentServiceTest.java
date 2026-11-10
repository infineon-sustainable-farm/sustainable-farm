package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.common.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Shipment;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.ShipmentRepository;
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
class ShipmentServiceTest {

    @Mock
    private ShipmentRepository repository;

    @InjectMocks
    private ShipmentService service;

    @Test
    void getAllReturnsRepositoryResults() {
        List<Shipment> shipments = List.of(new Shipment());
        when(repository.findAll()).thenReturn(shipments);

        assertSame(shipments, service.getAll());
    }

    @Test
    void getByIdThrowsWhenShipmentDoesNotExist() {
        when(repository.findById(42)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class, () -> service.getById(42));

        assertEquals("Shipment not found with id: 42", exception.getMessage());
    }

    @Test
    void createPropagatesRepositoryFailure() {
        Shipment shipment = new Shipment();
        when(repository.save(shipment)).thenThrow(new IllegalStateException("repository failure"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> service.create(shipment));

        assertEquals("repository failure", exception.getMessage());
        verify(repository).save(shipment);
    }
}
