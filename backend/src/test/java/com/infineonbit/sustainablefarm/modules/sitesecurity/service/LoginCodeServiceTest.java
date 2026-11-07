package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.core.notification.NotificationService;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.LoginCodeRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoginCodeServiceTest {

    private static final String EMAIL = "operator@example.test";
    private static final String SECRET = "s".repeat(32);

    @Mock
    private OperatorRepository operators;

    @Mock
    private LoginCodeRepository loginCodes;

    @Mock
    private NotificationService notifications;

    @Mock
    private PlatformTransactionManager transactionManager;

    private LoginCodeService service(String secret, boolean mailEnabled, String... profiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles);
        return new LoginCodeService(operators, loginCodes, notifications, transactionManager,
                environment, secret, mailEnabled);
    }

    private void knownOperator() {
        when(operators.lockActiveByEmail(EMAIL)).thenReturn(Optional.of(OperatorEntity.builder()
                .id("operator-id").email(EMAIL).role(SiteSecurityRole.READ).active(true).build()));
    }

    @Test
    void requestCode_shouldDoNothing_withoutASecret() {
        knownOperator();

        service("", true).requestCode(EMAIL);

        verifyNoInteractions(notifications, loginCodes);
    }

    @Test
    void requestCode_shouldDoNothing_withATooShortSecret() {
        knownOperator();

        service("s".repeat(31), true).requestCode(EMAIL);

        verifyNoInteractions(notifications, loginCodes);
    }

    @Test
    void verifyCode_shouldRefuseEverything_withoutASecret() {
        assertFalse(service("", true).verifyCode(EMAIL, "123456"));

        verifyNoInteractions(loginCodes);
    }

    @Test
    void requestCode_shouldNotUseTheConsoleSink_outsideDev() {
        knownOperator();

        service(SECRET, false, "prod").requestCode(EMAIL);

        verifyNoInteractions(notifications);
    }

    @Test
    void requestCode_shouldUseTheConsoleSink_inDev() {
        knownOperator();

        service(SECRET, false, "dev").requestCode(EMAIL);

        verify(notifications).send(anyString(), anyString(), anyString());
    }

    @Test
    void requestCode_shouldNotFail_whenTheMailLayerThrows() {
        knownOperator();
        when(notifications.send(anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("down"));

        assertDoesNotThrow(() -> service(SECRET, true).requestCode(EMAIL));
    }

    @Test
    void requestCode_shouldNotStoreACode_forAnUnknownEmail() {
        when(operators.lockActiveByEmail(EMAIL)).thenReturn(Optional.empty());

        service(SECRET, true).requestCode(EMAIL);

        verify(loginCodes, never()).save(any());
        verifyNoInteractions(notifications);
    }
}
