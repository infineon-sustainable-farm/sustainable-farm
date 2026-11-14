package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityPrincipal;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.CopiedLinkDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.HistoryActorDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.HistoryEntryDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.InviteUserRequestDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.UpdateUserRequestDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.UserDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.UserSummaryDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountTokenEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.AccountRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.AccountStatus;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.DisabledReason;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.EmailKind;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.Permission;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.TokenPurpose;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.UserCategory;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.EmailDeliveryEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecurityAuditAction;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecurityAuditEventEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.AccountTokenRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.EmailDeliveryRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.SiteSecurityAuditEventRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.ZoneRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin management of Farm Security accounts: invitations and their tracking,
 * roles, permissions, zones, validity, disable and enable. Creating an account
 * never grants physical access: the physical access register is separate.
 * The site keeps at least one active admin whose validity is running.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private static final Sort BY_EMAIL = Sort.by("email");

    private final OperatorRepository accounts;
    private final AccountTokenRepository tokens;
    private final EmailDeliveryRepository deliveries;
    private final ZoneRepository zones;
    private final SiteSecurityAuditEventRepository auditEvents;
    private final AccountTokens accountTokens;
    private final AccountEmailService emails;
    private final AccountLinks links;
    private final SessionService sessions;
    private final SiteSecurityAuditService audit;
    private final SignInThrottle throttle;

    // ---------------------------------------------------------------- reads

    public List<UserDto> list() {
        Instant now = Instant.now();
        return accounts.findAll(BY_EMAIL).stream().map(account -> view(account, now)).toList();
    }

    public UserSummaryDto summary() {
        Map<String, Long> counts = new HashMap<>();
        Instant now = Instant.now();
        for (OperatorEntity account : accounts.findAll()) {
            counts.merge(AccountViews.displayStatus(account, latestInvitation(account.getId()), now), 1L, Long::sum);
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return UserSummaryDto.builder()
                .pending(counts.getOrDefault(AccountViews.PENDING, 0L))
                .activationStarted(counts.getOrDefault(AccountViews.ACTIVATION_STARTED, 0L))
                .active(counts.getOrDefault(AccountViews.ACTIVE, 0L))
                .expired(counts.getOrDefault(AccountViews.EXPIRED, 0L))
                .revoked(counts.getOrDefault(AccountViews.REVOKED, 0L))
                .disabled(counts.getOrDefault(AccountViews.DISABLED, 0L))
                .total(total)
                .build();
    }

    /** Newest first: audit events about the account, with who made them (admin, the account itself, system). */
    public List<HistoryEntryDto> history(String id) {
        OperatorEntity account = find(id);
        Map<String, String> emailsById = new HashMap<>();
        accounts.findAll().forEach(a -> emailsById.put(a.getId(), a.getEmail()));
        List<SiteSecurityAuditEventEntity> events = new ArrayList<>(auditEvents.findByTargetIdOrderByOccurredAtAsc(account.getId()));
        Collections.reverse(events);
        return events.stream().map(event -> {
            String kind = event.getActorKind() == null ? SiteSecurityAuditService.ADMIN : event.getActorKind();
            String actorEmail = SiteSecurityAuditService.SYSTEM.equals(kind) ? null : emailsById.get(event.getActorOperatorId());
            return new HistoryEntryDto(event.getOccurredAt(), event.getAction().name().toLowerCase(Locale.ROOT),
                    new HistoryActorDto(kind.toLowerCase(Locale.ROOT), actorEmail), event.getDetail());
        }).toList();
    }

    public List<com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.EmailStateDto> emails(String id) {
        OperatorEntity account = find(id);
        return deliveries.findByAccountIdOrderByCreatedAtDesc(account.getId()).stream()
                .map(delivery -> AccountViews.emailState(delivery,
                        adminId -> accounts.findEmailById(adminId).orElse(null)))
                .toList();
    }

    // ---------------------------------------------------------------- invitations

    /** Creates a PENDING account (no access until activation) and emails a 7-day single-use link. */
    @Transactional
    public UserDto invite(InviteUserRequestDto request, SiteSecurityPrincipal actor) {
        String email = OperatorService.normalizeEmail(request.getEmail());
        if (!email.equals(OperatorService.normalizeEmail(request.getEmailConfirmation()))) {
            throw new BusinessRuleException("The two email addresses do not match.");
        }
        if (accounts.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists.");
        }
        AccountRole role = parse(AccountRole.class, request.getRole(), "role");
        UserCategory category = parse(UserCategory.class, request.getCategory(), "user type");
        Set<String> permissions = grantablePermissions(role, request.getPermissions(), false);
        Set<String> zoneIds = knownZones(role, request.getZoneIds());
        LocalDate validUntil = futureOrNull(request.getValidUntil());
        OperatorEntity account = OperatorEntity.builder()
                .id(UUID.randomUUID().toString())
                .email(email)
                .fullName(request.getFullName().trim())
                .category(category.name())
                .validUntil(validUntil)
                .createdBy(actor.actor())
                .createdAt(Instant.now())
                .permissions(permissions)
                .zoneIds(zoneIds)
                .build();
        account.applyRole(role);
        account.applyStatus(AccountStatus.PENDING);
        try {
            account = accounts.saveAndFlush(account);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("An account with this email already exists.");
        }
        sendInvitation(account, EmailKind.INVITATION, actor);
        audit.record(actor, SiteSecurityAuditAction.ACCOUNT_INVITED, account.getId(),
                AccountValues.api(role) + " " + AccountValues.api(category));
        return view(account, Instant.now());
    }

    /** A new link by email; the previous link stops working. Also a password setup link for a legacy account. */
    @Transactional
    public UserDto resendInvitation(String id, SiteSecurityPrincipal actor) {
        OperatorEntity account = lock(id);
        EmailKind kind = invitationKind(account);
        sendInvitation(account, kind, actor);
        audit.record(actor, SiteSecurityAuditAction.INVITATION_RESENT, id, AccountValues.api(kind));
        return view(account, Instant.now());
    }

    /**
     * Fallback when email does not work: a new link shown once to the admin, to
     * deliver by another channel. The previous link stops working. Audited.
     */
    @Transactional
    public CopiedLinkDto copyInvitationLink(String id, SiteSecurityPrincipal actor) {
        OperatorEntity account = lock(id);
        invitationKind(account);
        if (!links.isConfigured()) {
            throw new BusinessRuleException("No public address is configured for links.");
        }
        AccountTokens.Issued token = accountTokens.issue(account.getId(), TokenPurpose.INVITATION, actor.operatorId());
        audit.record(actor, SiteSecurityAuditAction.INVITATION_LINK_COPIED, id, null);
        return new CopiedLinkDto(links.activation(token.rawToken()), token.entity().getExpiresAt());
    }

    /** Withdraws a pending invitation: the link stops working and the account is marked revoked. */
    @Transactional
    public UserDto revokeInvitation(String id, SiteSecurityPrincipal actor) {
        OperatorEntity account = lock(id);
        if (account.effectiveStatus() != AccountStatus.PENDING) {
            throw new BusinessRuleException("Only a pending invitation can be revoked.");
        }
        Instant now = Instant.now();
        tokens.revokeAllOpen(id, now);
        account.applyStatus(AccountStatus.DISABLED);
        account.setDisabledReason(DisabledReason.INVITATION_REVOKED.name());
        account.setDisabledBy(actor.actor());
        account.setDisabledAt(now);
        accounts.save(account);
        audit.record(actor, SiteSecurityAuditAction.INVITATION_REVOKED, id, null);
        return view(account, now);
    }

    // ---------------------------------------------------------------- changes

    /** Name, role, user type, permissions, zones, validity. Last-admin rules apply. */
    @Transactional
    public UserDto update(String id, UpdateUserRequestDto request, SiteSecurityPrincipal actor) {
        List<OperatorEntity> admins = accounts.lockActiveByRole(SiteSecurityRole.ADMIN);
        OperatorEntity account = lock(id);
        account.adoptAccountFields();
        AccountRole oldRole = account.effectiveRole();
        AccountRole newRole = parse(AccountRole.class, request.getRole(), "role");
        UserCategory category = parse(UserCategory.class, request.getCategory(), "user type");
        // A field left out of the request keeps its value (a partial PATCH never removes access by accident).
        boolean keepPermissions = request.getPermissions() == null && newRole == AccountRole.USER && oldRole == AccountRole.USER;
        boolean keepZones = request.getZoneIds() == null && newRole == AccountRole.USER && oldRole == AccountRole.USER;
        Set<String> permissions = keepPermissions ? account.effectivePermissions()
                : grantablePermissions(newRole, request.getPermissions(),
                        account.effectivePermissions().contains(Permission.LEGACY_READ_ALL.name()));
        Set<String> zoneIds = keepZones ? account.effectiveZoneIds() : knownZones(newRole, request.getZoneIds());
        LocalDate validUntil = futureOrNull(request.getValidUntil());
        boolean losesAdmin = oldRole == AccountRole.ADMIN
                && (newRole != AccountRole.ADMIN || (validUntil != null && !Objects.equals(validUntil, account.getValidUntil())));
        if (losesAdmin) {
            rejectIfLastAdmin(account, admins, newRole != AccountRole.ADMIN
                    ? "The last active admin cannot be demoted."
                    : "The last active admin cannot be given an end date.");
        }
        List<String> changed = new ArrayList<>();
        String fullName = request.getFullName().trim();
        if (!fullName.equals(account.getFullName())) {
            changed.add("name");
        }
        if (!category.name().equals(account.getCategory())) {
            changed.add("type");
        }
        // Admins hold no permissions or zones: clearing them on promotion is part of the role change.
        if (newRole == AccountRole.USER && !permissions.equals(account.effectivePermissions())) {
            changed.add("permissions");
        }
        if (newRole == AccountRole.USER && !zoneIds.equals(account.effectiveZoneIds())) {
            changed.add("zones");
        }
        if (!Objects.equals(validUntil, account.getValidUntil())) {
            changed.add("validity");
        }
        account.setFullName(fullName);
        account.setCategory(category.name());
        account.setPermissions(new java.util.HashSet<>(permissions));
        account.setZoneIds(new java.util.HashSet<>(zoneIds));
        account.setValidUntil(validUntil);
        account.applyRole(newRole);
        accounts.save(account);
        if (oldRole != newRole) {
            audit.record(actor, SiteSecurityAuditAction.OPERATOR_ROLE_CHANGED, id,
                    AccountValues.api(oldRole) + "->" + AccountValues.api(newRole));
        }
        if (!changed.isEmpty()) {
            audit.record(actor, SiteSecurityAuditAction.ACCOUNT_UPDATED, id, String.join(",", changed));
        }
        return view(account, Instant.now());
    }

    /** Ends the account's sessions and open links at once. Idempotent. */
    @Transactional
    public UserDto disable(String id, SiteSecurityPrincipal actor) {
        List<OperatorEntity> admins = accounts.lockActiveByRole(SiteSecurityRole.ADMIN);
        OperatorEntity account = lock(id);
        if (account.effectiveStatus() == AccountStatus.DISABLED) {
            return view(account, Instant.now());
        }
        if (account.effectiveRole() == AccountRole.ADMIN) {
            rejectIfLastAdmin(account, admins, "The last active admin cannot be disabled.");
        }
        Instant now = Instant.now();
        boolean wasPending = account.effectiveStatus() == AccountStatus.PENDING;
        account.adoptAccountFields();
        account.applyStatus(AccountStatus.DISABLED);
        account.setDisabledReason(wasPending ? DisabledReason.ADMIN_PENDING.name() : DisabledReason.ADMIN.name());
        account.setDisabledBy(actor.actor());
        account.setDisabledAt(now);
        accounts.save(account);
        tokens.revokeAllOpen(id, now);
        sessions.revokeAll(id);
        throttle.forgetDevices(id, null);
        audit.record(actor, SiteSecurityAuditAction.OPERATOR_DISABLED, id, null);
        return view(account, now);
    }

    /**
     * Re-enables an account into the state it was disabled from: ACTIVE if it was
     * active (including legacy operators), PENDING if it had not activated yet
     * (it then needs a new invitation).
     */
    @Transactional
    public UserDto enable(String id, SiteSecurityPrincipal actor) {
        OperatorEntity account = lock(id);
        if (account.effectiveStatus() != AccountStatus.DISABLED) {
            return view(account, Instant.now());
        }
        String reason = account.getDisabledReason();
        boolean activatedBefore = !(DisabledReason.ADMIN_PENDING.name().equals(reason)
                || DisabledReason.INVITATION_REVOKED.name().equals(reason));
        account.adoptAccountFields();
        account.applyStatus(activatedBefore ? AccountStatus.ACTIVE : AccountStatus.PENDING);
        account.setDisabledReason(null);
        account.setDisabledBy(null);
        account.setDisabledAt(null);
        accounts.save(account);
        audit.record(actor, SiteSecurityAuditAction.OPERATOR_ENABLED, id, activatedBefore ? "active" : "pending");
        return view(account, Instant.now());
    }

    // ---------------------------------------------------------------- helpers

    private void sendInvitation(OperatorEntity account, EmailKind kind, SiteSecurityPrincipal actor) {
        AccountTokens.Issued token = accountTokens.issue(account.getId(), TokenPurpose.INVITATION, actor.operatorId());
        emails.queue(account, kind, token);
    }

    /** Invitation for a pending account, password setup link for an active account without password. */
    private static EmailKind invitationKind(OperatorEntity account) {
        AccountStatus status = account.effectiveStatus();
        if (status == AccountStatus.PENDING) {
            return EmailKind.INVITATION;
        }
        if (status == AccountStatus.ACTIVE && !account.hasPassword()) {
            return EmailKind.PASSWORD_SETUP;
        }
        throw new BusinessRuleException(status == AccountStatus.DISABLED
                ? "Enable the account before sending it a link."
                : "This account is already active with a password.");
    }

    private void rejectIfLastAdmin(OperatorEntity account, List<OperatorEntity> lockedAdmins, String message) {
        Instant now = Instant.now();
        long others = lockedAdmins.stream()
                .filter(admin -> !admin.getId().equals(account.getId()))
                .filter(admin -> admin.effectiveRole() == AccountRole.ADMIN && admin.usableAt(now))
                .filter(admin -> admin.getValidUntil() == null)
                .count();
        boolean accountCounts = account.effectiveRole() == AccountRole.ADMIN && account.usableAt(now);
        if (accountCounts && others == 0) {
            throw new BusinessRuleException(message);
        }
    }

    /** Only grantable permissions; LEGACY_READ_ALL may be kept by an account that has it, never added. */
    private Set<String> grantablePermissions(AccountRole role, List<String> requested, boolean mayKeepLegacy) {
        if (role == AccountRole.ADMIN || requested == null) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String value : requested) {
            Permission permission = parse(Permission.class, value, "permission");
            boolean keptLegacy = permission == Permission.LEGACY_READ_ALL && mayKeepLegacy;
            if (!Permission.GRANTABLE.contains(permission) && !keptLegacy) {
                throw new BusinessRuleException("This permission cannot be granted.");
            }
            result.add(permission.name());
        }
        return Set.copyOf(result);
    }

    private Set<String> knownZones(AccountRole role, List<String> requested) {
        if (role == AccountRole.ADMIN || requested == null) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String zoneId : requested) {
            String id = zoneId == null ? "" : zoneId.trim().toLowerCase(Locale.ROOT);
            if (!zones.existsById(id)) {
                throw new BusinessRuleException("Unknown zone '" + id + "'.");
            }
            result.add(id);
        }
        return Set.copyOf(result);
    }

    private static LocalDate futureOrNull(LocalDate validUntil) {
        if (validUntil != null && validUntil.isBefore(LocalDate.now(ZoneOffset.UTC))) {
            throw new BusinessRuleException("The end date must be today or later.");
        }
        return validUntil;
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, String label) {
        try {
            return AccountValues.parse(type, value);
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Unknown " + label + " '" + value + "'.");
        }
    }

    private AccountTokenEntity latestInvitation(String accountId) {
        List<AccountTokenEntity> list = tokens.findByAccountIdAndPurposeOrderByCreatedAtDesc(accountId,
                TokenPurpose.INVITATION.name());
        return list.isEmpty() ? null : list.get(0);
    }

    private UserDto view(OperatorEntity account, Instant now) {
        // The invitation email (or password setup link): notices sent to admins are not this account's invitation.
        EmailDeliveryEntity latestEmail = deliveries.findFirstByAccountIdAndKindInOrderByCreatedAtDesc(account.getId(),
                List.of(EmailKind.INVITATION.name(), EmailKind.PASSWORD_SETUP.name())).orElse(null);
        return AccountViews.toDto(account, latestInvitation(account.getId()), latestEmail, now);
    }

    private OperatorEntity find(String id) {
        return accounts.findById(id).orElseThrow(() -> new ResourceNotFoundException("No account with id '" + id + "'."));
    }

    private OperatorEntity lock(String id) {
        return accounts.lockById(id).orElseThrow(() -> new ResourceNotFoundException("No account with id '" + id + "'."));
    }
}
