package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ShipmentRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.ShipmentResponse;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private ShipmentRepository repository;

    @Mock
    private SalesMarketingRequestMapper mapper;

    @InjectMocks
    private ShipmentService service;

    @Test
    void getAllMapsEveryRowToItsResponse() {
        Shipment row = new Shipment();
        row.setId(7L);
        when(repository.findAll()).thenReturn(List.of(row));

        List<ShipmentResponse> all = service.getAll();

        assertEquals(1, all.size());
        assertEquals(7L, all.get(0).shipmentId());
    }

    @Test
    void getByIdThrowsWhenTheRowDoesNotExist() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> service.getById(42L));

        assertEquals("Shipment not found with id: 42", exception.getMessage());
    }

    @Test
    void createPropagatesRepositoryFailure() {
        ShipmentRequest request = new ShipmentRequest(null, null, "Abidjan", "Hamburg", null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
        Shipment entity = new Shipment();
        when(mapper.apply(any(ShipmentRequest.class), any(Shipment.class))).thenReturn(entity);
        when(repository.save(entity)).thenThrow(new IllegalStateException("repository failure"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.create(request));

        assertEquals("repository failure", exception.getMessage());
        verify(repository).save(entity);
    }
}
