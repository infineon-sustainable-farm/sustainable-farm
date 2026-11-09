package com.infineonbit.sustainablefarm.modules.energysupply.service;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.EnergyComponentRequest;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.ComponentCategory;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.ComponentStatus;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergyComponent;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.EnergyComponentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnergyComponentServiceTest {

    @Mock
    private EnergyComponentRepository energyComponentRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private EnergyComponentService service;

    @Test
    void createPersistsNewComponentWithoutMergeSemantics() {
        when(energyComponentRepository.existsById("ENG-006")).thenReturn(false);

        var response = service.create(request());

        assertEquals("ENG-006", response.componentId());
        verify(entityManager).persist(org.mockito.ArgumentMatchers.argThat(component ->
                component instanceof EnergyComponent energyComponent
                        && energyComponent.getComponentId().equals("ENG-006")));
        verify(entityManager).flush();
        verify(energyComponentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createRejectsAnExistingComponentIdWithoutChangingIt() {
        when(energyComponentRepository.existsById("ENG-006")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> service.create(request()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verifyNoInteractions(entityManager);
        verify(energyComponentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private EnergyComponentRequest request() {
        return new EnergyComponentRequest(
                "ENG-006", "Additional component", ComponentCategory.BATTERY,
                BigDecimal.ONE, ComponentStatus.OPERATIONAL);
    }
}
