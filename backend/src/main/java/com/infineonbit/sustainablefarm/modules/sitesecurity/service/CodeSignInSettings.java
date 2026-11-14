package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityPrincipal;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.CodeSignInConditionsDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.CodeSignInStatusDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.AccountRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.DeliveryStatus;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.EmailKind;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.SubmissionStatus;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.EmailDeliveryEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecurityAuditAction;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecuritySettingEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.AccountTokenRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.EmailDeliveryRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.SiteSecuritySettingRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The legacy email-code sign-in (migration stages 0 to 2) and its reversible
 * retirement (stage 3). Retirement is refused until real email delivery has
 * been verified, at least two admins sign in with a password, and every active
 * admin has one. Retiring revokes no session.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodeSignInSettings {

    static final String KEY = "CODE_SIGN_IN_RETIRED";
    static final String CONFIRMATION = "RETIRE";
    static final int REQUIRED_ADMINS_WITH_PASSWORD_SIGN_IN = 2;
    static final String TARGET = "code-sign-in";
    /** Audit detail prefix of an admin's delivery attestation ("manual:KIND:deliveryId", at most 64 chars). */
    static final String MANUAL_ATTESTATION = "manual";

    private final SiteSecuritySettingRepository settings;
    private final OperatorRepository accounts;
    private final EmailDeliveryRepository deliveries;
    private final AccountTokenRepository tokens;
    private final SiteSecurityAuditService audit;

    public boolean isRetired() {
        return settings.findById(KEY).map(s -> "true".equals(s.getValue())).orElse(false);
    }

    public CodeSignInStatusDto status() {
        Instant now = Instant.now();
        List<OperatorEntity> activeAdmins = accounts.findAll().stream()
                .filter(a -> a.effectiveRole() == AccountRole.ADMIN && a.usableAt(now))
                .toList();
        long withPasswordSignIn = activeAdmins.stream()
                .filter(a -> a.hasPassword() && a.getLastPasswordLoginAt() != null)
                .count();
        boolean allHavePassword = !activeAdmins.isEmpty() && activeAdmins.stream().allMatch(OperatorEntity::hasPassword);
        boolean delivered = verifiedAndUsed(List.of(EmailKind.INVITATION.name(), EmailKind.PASSWORD_SETUP.name()))
                && verifiedAndUsed(List.of(EmailKind.PASSWORD_RESET.name()));
        CodeSignInConditionsDto conditions = CodeSignInConditionsDto.builder()
                .realDeliveryVerified(delivered)
                .adminsWithPasswordSignIn(withPasswordSignIn)
                .requiredAdminsWithPasswordSignIn(REQUIRED_ADMINS_WITH_PASSWORD_SIGN_IN)
                .allActiveAdminsHavePassword(allHavePassword)
                .build();
        boolean canRetire = delivered && withPasswordSignIn >= REQUIRED_ADMINS_WITH_PASSWORD_SIGN_IN && allHavePassword;
        return CodeSignInStatusDto.builder().retired(isRetired()).conditions(conditions).canRetire(canRetire).build();
    }

    @Transactional
    public CodeSignInStatusDto retire(SiteSecurityPrincipal actor, String confirmation) {
        if (!CONFIRMATION.equals(confirmation)) {
            throw new BusinessRuleException("Type RETIRE to confirm.");
        }
        CodeSignInStatusDto current = status();
        if (current.isRetired()) {
            return current;
        }
        if (!current.isCanRetire()) {
            throw new BusinessRuleException("Email-code sign-in cannot be retired yet: the conditions are not all met.");
        }
        save(actor, "true");
        audit.record(actor, SiteSecurityAuditAction.CODE_SIGN_IN_RETIRED, TARGET, null);
        return status();
    }

    @Transactional
    public CodeSignInStatusDto restore(SiteSecurityPrincipal actor) {
        if (isRetired()) {
            save(actor, "false");
            audit.record(actor, SiteSecurityAuditAction.CODE_SIGN_IN_RESTORED, TARGET, null);
        }
        return status();
    }

    /**
     * An admin attests that an email reached a real external inbox (with evidence,
     * for example a provider log id). This is a manual attestation, never a
     * provider-confirmed delivery: it is stored as CONFIRMED_BY_ADMIN with who,
     * when and the evidence. Only an email the provider accepted (SUBMITTED) can
     * be attested; one that was never sent (failed, disabled, console) cannot.
     */
    @Transactional
    public void confirmDelivery(SiteSecurityPrincipal actor, String deliveryId, String evidence) {
        EmailDeliveryEntity delivery = deliveries.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("No email with id '" + deliveryId + "'."));
        if (DeliveryStatus.CONFIRMED_BY_ADMIN.name().equals(delivery.getDeliveryStatus())) {
            return;
        }
        if (!SubmissionStatus.SUBMITTED.name().equals(delivery.getSubmissionStatus())) {
            throw new BusinessRuleException("Only an email submitted to the provider can be confirmed as delivered.");
        }
        delivery.setDeliveryStatus(DeliveryStatus.CONFIRMED_BY_ADMIN.name());
        delivery.setDeliveryConfirmedAt(Instant.now());
        delivery.setDeliveryConfirmedBy(actor.operatorId());
        delivery.setDeliveryEvidence(evidence.trim());
        deliveries.save(delivery);
        audit.record(actor, SiteSecurityAuditAction.EMAIL_DELIVERY_CONFIRMED, delivery.getAccountId(),
                MANUAL_ATTESTATION + ":" + delivery.getKind() + ":" + delivery.getId());
    }

    /** At least one email of these kinds was submitted, attested delivered by an admin, AND its link was then used. */
    private boolean verifiedAndUsed(List<String> kinds) {
        return deliveries.findByKindInAndDeliveryStatus(kinds, DeliveryStatus.CONFIRMED_BY_ADMIN.name()).stream()
                .filter(d -> SubmissionStatus.SUBMITTED.name().equals(d.getSubmissionStatus()))
                .filter(d -> d.getTokenId() != null)
                .anyMatch(d -> tokens.findById(d.getTokenId()).map(t -> t.getUsedAt() != null).orElse(false));
    }

    private void save(SiteSecurityPrincipal actor, String value) {
        settings.save(SiteSecuritySettingEntity.builder()
                .key(KEY).value(value).updatedBy(actor.operatorId()).updatedAt(Instant.now()).build());
    }
}
