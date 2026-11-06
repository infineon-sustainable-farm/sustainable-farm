package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteSecurityBootstrapAdminSeederTest {

    @Mock
    private OperatorRepository operators;

    @Test
    void run_shouldCreateAnActiveAdmin_withANormalizedEmail() {
        when(operators.existsByEmail("first.admin@example.test")).thenReturn(false);

        new SiteSecurityBootstrapAdminSeeder(operators, "  First.Admin@Example.TEST ").run();

        ArgumentCaptor<OperatorEntity> saved = ArgumentCaptor.forClass(OperatorEntity.class);
        verify(operators).save(saved.capture());
        assertEquals("first.admin@example.test", saved.getValue().getEmail());
        assertEquals(SiteSecurityRole.ADMIN, saved.getValue().getRole());
        assertEquals(SiteSecurityBootstrapAdminSeeder.BOOTSTRAP_ACTOR, saved.getValue().getCreatedBy());
        assertTrue(saved.getValue().isActive());
    }

    @Test
    void run_shouldNotTouchAnExistingOperator() {
        when(operators.existsByEmail("first.admin@example.test")).thenReturn(true);

        new SiteSecurityBootstrapAdminSeeder(operators, "first.admin@example.test").run();

        verify(operators, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "not-an-email", "two@at@example.test", "a b@example.test"})
    void run_shouldCreateNobody_whenTheEmailIsMissingOrInvalid(String email) {
        new SiteSecurityBootstrapAdminSeeder(operators, email).run();

        verify(operators, never()).save(any());
    }
}
