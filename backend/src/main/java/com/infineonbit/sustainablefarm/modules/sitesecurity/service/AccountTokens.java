package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountTokenEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.TokenPurpose;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.AccountTokenRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates and looks up single-use account links. A link carries 32 random bytes
 * (base64url); only their SHA-256 hash is stored. Such a value has 256 bits of
 * entropy, so a fast hash is enough (unlike passwords).
 */
@Component
@RequiredArgsConstructor
public class AccountTokens {

    public static final Duration INVITATION_VALIDITY = Duration.ofDays(7);
    public static final Duration RESET_VALIDITY = Duration.ofMinutes(30);
    private static final int TOKEN_BYTES = 32;
    private static final int MAX_TOKEN_LENGTH = 128;

    private final AccountTokenRepository repository;
    private final SecureRandom random = new SecureRandom();

    /** A token and its stored row. The raw value must only travel in the email or once to an admin. */
    public record Issued(String rawToken, AccountTokenEntity entity) {
        @Override
        public String toString() {
            return "Issued[tokenId=" + entity.getId() + "]";
        }
    }

    /** Revokes the account's open links of this purpose and issues a new one. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Issued issue(String accountId, TokenPurpose purpose, String createdBy) {
        Duration validity = purpose == TokenPurpose.INVITATION ? INVITATION_VALIDITY : RESET_VALIDITY;
        return issue(accountId, purpose, createdBy, Instant.now().plus(validity));
    }

    /** A new link of this purpose that leaves the account's other open links valid (password resets). */
    @Transactional(propagation = Propagation.MANDATORY)
    public Issued issueAlongside(String accountId, TokenPurpose purpose, String createdBy) {
        Duration validity = purpose == TokenPurpose.INVITATION ? INVITATION_VALIDITY : RESET_VALIDITY;
        return create(accountId, purpose, createdBy, Instant.now().plus(validity));
    }

    /** A mail retry: the link it replaces is revoked (only that one) and the new one keeps its expiry. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Issued replace(AccountTokenEntity old) {
        repository.revokeOne(old.getId(), Instant.now());
        return create(old.getAccountId(), TokenPurpose.valueOf(old.getPurpose()), old.getCreatedBy(), old.getExpiresAt());
    }

    /** With a given expiry: a mail retry keeps the expiry of the link it replaces. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Issued issue(String accountId, TokenPurpose purpose, String createdBy, Instant expiresAt) {
        repository.revokeOpen(accountId, purpose.name(), Instant.now());
        return create(accountId, purpose, createdBy, expiresAt);
    }

    private Issued create(String accountId, TokenPurpose purpose, String createdBy, Instant expiresAt) {
        Instant now = Instant.now();
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        AccountTokenEntity entity = repository.save(AccountTokenEntity.builder()
                .id(UUID.randomUUID().toString())
                .accountId(accountId)
                .purpose(purpose.name())
                .tokenHash(SessionService.hash(raw))
                .createdAt(now)
                .expiresAt(expiresAt)
                .createdBy(createdBy)
                .build());
        return new Issued(raw, entity);
    }

    /** The stored row of a raw token, if the value has a plausible shape. */
    public Optional<AccountTokenEntity> find(String rawToken) {
        return plausible(rawToken) ? repository.findByTokenHash(SessionService.hash(rawToken)) : Optional.empty();
    }

    /** The account a raw token belongs to, without loading the token row. */
    public Optional<String> accountIdOf(String rawToken) {
        return plausible(rawToken) ? repository.findAccountIdByTokenHash(SessionService.hash(rawToken)) : Optional.empty();
    }

    /** Same, with the row locked for the rest of the transaction. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<AccountTokenEntity> lock(String rawToken) {
        return plausible(rawToken) ? repository.lockByTokenHash(SessionService.hash(rawToken)) : Optional.empty();
    }

    private static boolean plausible(String rawToken) {
        return rawToken != null && !rawToken.isBlank() && rawToken.length() <= MAX_TOKEN_LENGTH
                && rawToken.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '-' || c == '_');
    }
}
