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
 * <p>Only production classes are checked. The violations that existed when the rules were
 * introduced are frozen in {@code src/test/resources/archunit_store} ({@link FreezingArchRule},
 * configured by {@code src/test/resources/archunit.properties}): they are tolerated, any new
 * violation fails the test. The store is never updated silently:</p>
 *
 * <ul>
 *   <li>a new violation fails: fix the code, do not add it to the store;</li>
 *   <li>a fixed violation also fails, with "Updating frozen violations is not allowed": remove
 *       it from the store, either by hand or by running the test once with
 *       {@code -Darchunit.freeze.store.default.allowStoreUpdate=true}, and commit the store.</li>
 * </ul>
 *
 * <p>Frozen violations, removed by the tasks of the integration plan:</p>
 *
 * <ul>
 *   <li>watersupply {@code PlantsGrowthCalendarAdapter} uses plants {@code GrowthCalendarService}
 *       and its response DTO instead of a {@code plants.api} interface (T15, T16);</li>
 *   <li>sitesecurity {@code SiteSecurityExceptionHandler}, kept while the module is excluded (Q6,
 *       T43).</li>
 * </ul>
 *
 * <p>Tasks T15 to T25 remove the frozen violations, T47 removes the freeze: the rules then
 * apply without exception.</p>
 */
@AnalyzeClasses(
        packages = ModuleBoundariesTest.ROOT,
        importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundariesTest {

    static final String ROOT = "com.infineonbit.sustainablefarm";
    private static final String MODULES = ROOT + ".modules.";
    private static final String CORE = ROOT + ".core..";

    @ArchTest
    static final ArchRule modules_depend_on_other_modules_only_through_their_api = FreezingArchRule.freeze(
            classes().that().resideInAPackage(ROOT + ".modules..")
                    .should(dependOnOtherModulesOnlyThroughTheirApi())
                    .as("modules depend on other modules only through their api package"));

    @ArchTest
    static final ArchRule core_depends_on_no_module =
            noClasses().that().resideInAPackage(CORE)
                    .should().dependOnClassesThat().resideInAPackage(ROOT + ".modules..")
                    .as("core depends on no module");

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
