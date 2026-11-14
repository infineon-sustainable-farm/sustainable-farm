package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.AccountStatus;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.DisabledReason;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.Permission;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.AccountRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Migration stage 0 (additive): gives operators created before accounts existed
 * their account fields, without taking anything away. Idempotent: rows that
 * already have a status are left alone.
 *
 * <ul>
 *   <li>Active operators stay ACTIVE and keep their sessions and email-code sign-in.</li>
 *   <li>Admins stay ADMIN.</li>
 *   <li>Operators of the old read-only role become USER with LEGACY_READ_ALL: the
 *       same read-only views as before, and no zone assignment (zones are never
 *       granted automatically).</li>
 *   <li>Disabled operators stay DISABLED (disabled by an admin).</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SiteSecurityAccountMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SiteSecurityAccountMigration.class);

    private final OperatorRepository accounts;

    public SiteSecurityAccountMigration(OperatorRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<OperatorEntity> legacy = accounts.findAll().stream().filter(a -> a.getAccountStatus() == null).toList();
        int admins = 0;
        int readOnly = 0;
        for (OperatorEntity account : legacy) {
            AccountRole role = account.effectiveRole();
            AccountStatus status = account.effectiveStatus();
            if (role == AccountRole.USER) {
                HashSet<String> permissions = new HashSet<>(account.getPermissions() == null ? List.of() : account.getPermissions());
                permissions.add(Permission.LEGACY_READ_ALL.name());
                account.setPermissions(permissions);
                readOnly++;
            } else {
                admins++;
            }
            account.applyRole(role);
            account.applyStatus(status);
            if (status == AccountStatus.DISABLED && account.getDisabledReason() == null) {
                account.setDisabledReason(DisabledReason.ADMIN.name());
            }
            accounts.save(account);
        }
        if (!legacy.isEmpty()) {
            log.info("Farm Security accounts migrated (stage 0): {} admin(s), {} read-only operator(s); "
                    + "sessions and email-code sign-in unchanged.", admins, readOnly);
        }
    }
}
