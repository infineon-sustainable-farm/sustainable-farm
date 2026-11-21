package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Foreign keys from Site Security to the people referential of the staff module (ADR 0004),
 * created by {@code V2__sitesecurity_shared_person_and_authorization.sql} and checked at startup
 * by {@code core.schema.CrossModuleForeignKeyInstaller}: a person who holds a Site Security role,
 * a credential or a logged passage cannot be deleted.
 */
@Configuration
public class SiteSecurityPersonForeignKeys {

    @Bean
    CrossModuleForeignKey siteSecurityOperatorPersonForeignKey() {
        return toPerson("fk_site_security_operators_person", "site_security_operators");
    }

    @Bean
    CrossModuleForeignKey siteSecurityCredentialPersonForeignKey() {
        return toPerson("fk_site_security_users_person", "site_security_users");
    }

    @Bean
    CrossModuleForeignKey siteSecurityLogPersonForeignKey() {
        return toPerson("fk_site_security_logs_person", "site_security_logs");
    }

    private static CrossModuleForeignKey toPerson(String name, String table) {
        return new CrossModuleForeignKey(name, table, "person_id", PersonDirectory.TABLE, PersonDirectory.KEY_COLUMN);
    }
}
