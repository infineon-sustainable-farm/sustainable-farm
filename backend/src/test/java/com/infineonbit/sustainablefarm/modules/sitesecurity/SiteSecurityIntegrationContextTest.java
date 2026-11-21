package com.infineonbit.sustainablefarm.modules.sitesecurity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.modules.sitesecurity.controller.SiteSecurityController;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.CredentialUserEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.LogEntryEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecurityOperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.CredentialUserRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.LogEntryRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.SiteSecurityOperatorRepository;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDetails;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Site Security in the whole application: it starts next to Product Transformation (which has
 * its own operators) without bean name collisions, stores people as references to the shared
 * {@code person} table, and keeps no copy of their names or emails.
 */
@SpringBootTest
@ActiveProfiles("test")
class SiteSecurityIntegrationContextTest {

    @Autowired private ApplicationContext context;
    @Autowired private PersonDirectory people;
    @Autowired private SiteSecurityOperatorRepository operators;
    @Autowired private CredentialUserRepository credentials;
    @Autowired private LogEntryRepository logs;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void startsWithProductTransformation_withModuleScopedBeanNames() {
        // Bean definition overriding is off by default: a duplicate bean name fails this startup.
        assertTrue(context.containsBean("siteSecurityOperatorRepository"));
        assertFalse(context.getBeansOfType(SiteSecurityController.class).isEmpty());
        assertTrue(java.util.Arrays.stream(context.getBeanDefinitionNames())
                .filter(name -> context.getType(name) != null
                        && context.getType(name).getPackageName().contains(".sitesecurity"))
                .noneMatch(name -> name.equals("operatorRepository") || name.equals("operatorService")),
                "Site Security beans carry module-scoped names");
        assertTrue(java.util.Arrays.stream(context.getBeanDefinitionNames())
                .anyMatch(name -> context.getType(name) != null
                        && context.getType(name).getPackageName().contains(".producttransformation")),
                "Product Transformation is loaded in the same context");
    }

    @Test
    void declaresItsForeignKeysToThePeopleDirectory() {
        List<String> names = context.getBeansOfType(CrossModuleForeignKey.class).values().stream()
                .filter(key -> key.table().startsWith("site_security_"))
                .map(CrossModuleForeignKey::name).sorted().toList();

        assertEquals(List.of("fk_site_security_logs_person", "fk_site_security_operators_person",
                "fk_site_security_users_person"), names);
    }

    @Test
    void theTablesHoldNoCopyOfPersonalData() {
        // H2 (local) and PostgreSQL (CI) differ in identifier case: compare in lower case, current schema only.
        for (String table : List.of("site_security_users", "site_security_logs", "site_security_operators")) {
            List<String> columns = jdbc.queryForList(
                    "SELECT lower(column_name) FROM information_schema.columns"
                            + " WHERE lower(table_name) = ? AND table_schema = current_schema()", String.class, table);
            assertTrue(columns.contains("person_id"), table + " references person");
            for (String copied : List.of("name", "email", "initials", "user_name", "full_name", "central_user_id")) {
                assertFalse(columns.contains(copied), table + " must not copy " + copied);
            }
        }
    }

    @Test
    @Transactional
    void referencesAnExistingPerson_andRefusesAnUnknownOne() {
        Long personId = people.create(new PersonDetails("Site Security Tester", "ss.tester@example.test", null, null)).id();

        SiteSecurityOperatorEntity operator = operators.saveAndFlush(SiteSecurityOperatorEntity.builder()
                .personId(personId).role(SiteSecurityRole.USER)
                .permissions(Set.of("VIEW_ZONES")).zoneIds(Set.of("open")).build());
        credentials.saveAndFlush(CredentialUserEntity.builder().id("it-1").personId(personId)
                .role("Tester").type("Staff").level("open").validUntil("Permanent").status("Active").build());
        logs.saveAndFlush(LogEntryEntity.builder().id("it-log").personId(null).status("Denied").zone("open").build());

        assertEquals(personId, operators.findByPersonId(personId).orElseThrow().getPersonId());
        assertEquals(Set.of("open"), operators.findById(operator.getId()).orElseThrow().getZoneIds());
        assertThrows(DataIntegrityViolationException.class, () -> credentials.saveAndFlush(
                CredentialUserEntity.builder().id("it-2").personId(987654L)
                        .role("Ghost").type("Staff").level("open").validUntil("Permanent").status("Active").build()));
    }
}
