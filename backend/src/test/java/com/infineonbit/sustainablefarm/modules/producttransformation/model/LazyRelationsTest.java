package com.infineonbit.sustainablefarm.modules.producttransformation.model;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * Every to-one association that product transformation owns is lazy: an eager one loads its target
 * row for each row of a list (N+1). The read queries fetch what the responses show with an entity
 * graph, and the responses are built inside a read-only transaction.
 */
class LazyRelationsTest {

    private static final String MODULE = "com.infineonbit.sustainablefarm.modules.producttransformation";

    @Test
    void ownedToOneAssociationsAreLazy() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        List<String> owned = new ArrayList<>();
        List<String> eager = new ArrayList<>();
        for (BeanDefinition candidate : scanner.findCandidateComponents(MODULE)) {
            for (Field field : Class.forName(candidate.getBeanClassName()).getDeclaredFields()) {
                String name = field.getDeclaringClass().getSimpleName() + "." + field.getName();
                ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
                OneToOne oneToOne = field.getAnnotation(OneToOne.class);
                if (manyToOne != null) {
                    owned.add(name);
                    if (manyToOne.fetch() != FetchType.LAZY) {
                        eager.add(name);
                    }
                } else if (oneToOne != null && oneToOne.mappedBy().isEmpty()) {
                    owned.add(name);
                    if (oneToOne.fetch() != FetchType.LAZY) {
                        eager.add(name);
                    }
                }
            }
        }

        assertThat(owned).contains("DryingRun.batch", "DryingRun.operator", "WashSortRecord.batch",
                "PackagingRecord.batch", "QcCheckpoint.inspector", "ComplianceRecord.auditor", "RawIntake.batch");
        assertThat(eager).isEmpty();
    }
}
