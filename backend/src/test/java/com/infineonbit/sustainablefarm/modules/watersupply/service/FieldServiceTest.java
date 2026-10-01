package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.FieldCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Field;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FieldServiceTest {
    @Mock
    private FarmRepository farmRepository;

    @Mock
    private FieldRepository fieldRepository;

    @Test
    void createFieldRejectsUnknownFarm() {
        UUID farmId = UUID.randomUUID();
        FieldCreateRequest request = new FieldCreateRequest(farmId, "Field", 1.0, null, null, null);
        when(farmRepository.existsById(farmId)).thenReturn(false);
        FieldService service = new FieldService(farmRepository, fieldRepository);

        assertThrows(ResourceNotFoundException.class, () -> service.createField(request));
        verify(fieldRepository, never()).save(org.mockito.ArgumentMatchers.any(Field.class));
    }
}