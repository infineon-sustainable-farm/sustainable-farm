package com.infineonbit.sustainablefarm.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Module boundaries of the modular monolith (docs/adr/0003-communication-entre-modules.md).
 *
 * <ul>
 *   <li>A class of {@code modules.<x>} depends on another module {@code modules.<y>} only
 *       through its public package {@code modules.<y>.api}.</li>
 *   <li>{@code core} depends on no module.</li>
 *   <li>Errors are handled only in {@code core}: no {@code @ControllerAdvice} or
 *       {@code @RestControllerAdvice}, and no class named {@code *ExceptionHandler}, elsewhere.</li>
 * </ul>
 *
 * <p>Only production classes are checked. The first two rules are strict. The third one is
 * frozen ({@link FreezingArchRule}, store in {@code src/test/resources/archunit_store},
 * configured by {@code src/test/resources/archunit.properties}) for one class only: the
 * {@code SiteSecurityExceptionHandler} of the sitesecurity module, excluded from the
 * integration (plan Q6, T43). The freeze is lifted, and its store entry removed, when
 * sitesecurity is integrated. The store is never updated silently: a new violation fails, and
 * so does a fixed one until its entry is removed from the store.</p>
 */
@AnalyzeClasses(
        packages = ModuleBoundariesTest.ROOT,
        importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundariesTest {

    static final String ROOT = "com.infineonbit.sustainablefarm";
    private static final String MODULES = ROOT + ".modules.";
    private static final String CORE = ROOT + ".core..";

    @ArchTest
    static final ArchRule modules_depend_on_other_modules_only_through_their_api =
            classes().that().resideInAPackage(ROOT + ".modules..")
                    .should(dependOnOtherModulesOnlyThroughTheirApi())
                    .as("modules depend on other modules only through their api package");

    @ArchTest
    static final ArchRule core_depends_on_no_module =
            noClasses().that().resideInAPackage(CORE)
                    .should().dependOnClassesThat().resideInAPackage(ROOT + ".modules..")
                    .as("core depends on no module");

    /** Frozen for sitesecurity SiteSecurityExceptionHandler only, until sitesecurity is integrated. */
    @ArchTest
    static final ArchRule errors_are_handled_only_in_core = FreezingArchRule.freeze(
            noClasses().that().resideOutsideOfPackage(CORE)
                    .should().beMetaAnnotatedWith(ControllerAdvice.class)
                    .orShould().beAnnotatedWith(ControllerAdvice.class)
                    .orShould().haveSimpleNameEndingWith("ExceptionHandler")
                    .as("errors are handled only in core"));

    private static ArchCondition<JavaClass> dependOnOtherModulesOnlyThroughTheirApi() {
        return new ArchCondition<>("depend on other modules only through their api package") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                String source = moduleOf(javaClass);
                for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass().getBaseComponentType();
                    String module = moduleOf(target);
                    if (module == null || module.equals(source)) {
                        continue;
                    }
                    String api = MODULES + module + ".api";
                    String pkg = target.getPackageName();
                    if (!pkg.equals(api) && !pkg.startsWith(api + ".")) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    /** The module of a class ({@code modules.<module>...}), or null outside the modules. */
    private static String moduleOf(JavaClass javaClass) {
        String pkg = javaClass.getPackageName();
        if (!pkg.startsWith(MODULES)) {
            return null;
        }
        String rest = pkg.substring(MODULES.length());
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }
}
