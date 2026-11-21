package com.infineonbit.sustainablefarm.modules.energysupply.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.EnergyComponentRequest;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.ComponentCategory;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.ComponentStatus;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergyComponent;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.EnergyComponentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnergyComponentServiceTest {

    @Mock
    private EnergyComponentRepository energyComponentRepository;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private EnergyComponentService service;

    @Test
    void createSavesTheComponentUnderItsBusinessCode() {
        when(energyComponentRepository.existsByCode("ENG-006")).thenReturn(false);
        when(energyComponentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(request());

        assertEquals("ENG-006", response.componentId());
        verify(energyComponentRepository).save(argThat(component -> "ENG-006".equals(component.getCode())));
    }

    @Test
    void createRejectsAnExistingCodeWithoutChangingIt() {
        when(energyComponentRepository.existsByCode("ENG-006")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> service.create(request()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(energyComponentRepository, never()).save(any());
    }

    @Test
    void deleteRefusesAComponentThatAnAlertIsAbout() {
        EnergyComponent component = component("ENG-002");
        when(energyComponentRepository.findByCode("ENG-002")).thenReturn(Optional.of(component));
        when(alertService.isAboutComponent(component)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.delete("ENG-002"));

        verify(energyComponentRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesAComponentWithoutAlerts() {
        EnergyComponent component = component("ENG-006");
        when(energyComponentRepository.findByCode("ENG-006")).thenReturn(Optional.of(component));
        when(alertService.isAboutComponent(component)).thenReturn(false);

        service.delete("ENG-006");

        verify(energyComponentRepository).delete(component);
    }

    private static EnergyComponent component(String code) {
        EnergyComponent component = new EnergyComponent();
        component.setId(7L);
        component.setCode(code);
        return component;
    }

    private EnergyComponentRequest request() {
        return new EnergyComponentRequest(
                "ENG-006", "Additional component", ComponentCategory.BATTERY,
                BigDecimal.ONE, ComponentStatus.OPERATIONAL);
    }
}
