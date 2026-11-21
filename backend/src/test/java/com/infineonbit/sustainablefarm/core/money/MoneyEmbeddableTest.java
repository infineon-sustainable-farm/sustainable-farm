package com.infineonbit.sustainablefarm.core.money;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link Money} embedded in an entity, on the columns {@code <prefix>_amount} and
 * {@code <prefix>_currency} named with {@code @AttributeOverride} (ADR 0002).
 */
@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = MoneyEmbeddableTest.JpaTestConfiguration.class)
class MoneyEmbeddableTest {

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = MoneyEmbeddableTest.class)
    @EnableJpaRepositories(basePackageClasses = MoneyEmbeddableTest.class)
    static class JpaTestConfiguration {
    }

    /** Test-only entity, the pattern a module follows to declare a money field. */
    @Entity
    @Table(name = "money_probe")
    static class MoneyProbe {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        Long id;

        @Embedded
        @AttributeOverride(name = "amount", column = @Column(name = "price_amount", precision = 14, scale = 2))
        @AttributeOverride(name = "currency", column = @Column(name = "price_currency", length = 3))
        Money price;
    }

    @Autowired
    private EntityManager entityManager;

    private MoneyProbe saveAndReload(Money price) {
        MoneyProbe probe = new MoneyProbe();
        probe.price = price;
        entityManager.persist(probe);
        entityManager.flush();
        entityManager.clear();
        return entityManager.find(MoneyProbe.class, probe.id);
    }

    @Test
    void storesTheAmountAndTheCurrencyNameInThePrefixedColumns() {
        MoneyProbe reloaded = saveAndReload(Money.of("78714.84", CurrencyCode.XOF));

        assertEquals(Money.of("78714.84", CurrencyCode.XOF), reloaded.price);
        List<?> row = List.of((Object[]) entityManager.createNativeQuery(
                "select price_amount, price_currency from money_probe where id = " + reloaded.id)
                .getSingleResult());
        assertEquals(0, new BigDecimal("78714.84").compareTo((BigDecimal) row.get(0)));
        assertEquals("XOF", row.get(1));
    }

    @Test
    void readsAnEmptyAmountAsNoMoney() {
        assertNull(saveAndReload(null).price);
    }
}
