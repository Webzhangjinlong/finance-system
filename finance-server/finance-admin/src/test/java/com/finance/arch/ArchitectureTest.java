package com.finance.arch;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.base.DescribedPredicate.doNot;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * 架构守护测试（AGENTS.md 硬约束的机器强制）。
 *
 * <p>规则空转期即可合入：当前无业务类时全部通过；后续任何代码违反
 * 依赖方向 / 模块边界 / 命名 / 金额类型时，CI 将直接失败。</p>
 */
@AnalyzeClasses(packages = "com.finance")
public class ArchitectureTest {

    private static final Pattern MODULE_PATTERN = Pattern.compile("^com\\.finance\\.(\\w+)");

    private static final Set<String> BUSINESS_MODULES =
            Set.of("system", "finance", "contract", "hr", "workflow");

    /** 取类所属模块（com.finance.<module>），非业务模块返回 null。 */
    private static String moduleOf(JavaClass clazz) {
        Matcher m = MODULE_PATTERN.matcher(clazz.getPackageName());
        return m.find() ? m.group(1) : null;
    }

    // ===== 硬约束 10：依赖方向 Controller → Service → Mapper，禁止跨层 =====

    @ArchTest
    static final ArchRule layered_dependencies =
            layeredArchitecture()
                    .consideringAllDependencies()
                    .withOptionalLayers(true)
                    .layer("Controller").definedBy("com.finance..controller..")
                    .layer("Service").definedBy("com.finance..service..")
                    .layer("Mapper").definedBy("com.finance..mapper..")
                    .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
                    .whereLayer("Mapper").mayOnlyBeAccessedByLayers("Service");

    @ArchTest
    static final ArchRule controller_never_touches_controller_or_mapper =
            noClasses().that().haveSimpleNameEndingWith("Controller")
                    .should().dependOnClassesThat(
                            simpleNameEndingWith("Controller")
                                    .and(doNot(simpleName("RestController"))))
                    .orShould().dependOnClassesThat().haveSimpleNameEndingWith("Mapper")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule service_never_touches_controller =
            noClasses().that().haveSimpleNameEndingWith("Service")
                    .should().dependOnClassesThat(
                            simpleNameEndingWith("Controller")
                                    .and(doNot(simpleName("RestController"))))
                    .allowEmptyShould(true);

    // ===== 硬约束 10 后半：禁止跨域模块直调他人 Mapper（走对方 Service） =====

    @ArchTest
    static final ArchRule no_cross_module_mapper_dependency =
            noClasses()
                    .that(describe("reside in a business module",
                            c -> {
                                String m = moduleOf(c);
                                return m != null && BUSINESS_MODULES.contains(m);
                            }))
                    .should(new ArchCondition<JavaClass>("not depend on a Mapper of another module") {
                        @Override
                        public void check(JavaClass clazz, ConditionEvents events) {
                            for (Dependency dep : clazz.getDirectDependenciesFromSelf()) {
                                JavaClass target = dep.getTargetClass();
                                String srcModule = moduleOf(clazz);
                                String dstModule = moduleOf(target);
                                boolean isMapper = target.getSimpleName().endsWith("Mapper");
                                if (isMapper && srcModule != null && dstModule != null
                                        && !srcModule.equals(dstModule)) {
                                    events.add(SimpleConditionEvent.violated(dep,
                                            clazz.getName() + " 跨模块直调 Mapper: " + target.getName()
                                                    + "（应通过对方模块 Service）"));
                                }
                            }
                        }
                    })
                    .allowEmptyShould(true);

    // ===== 硬约束 11：业务代码落在对应模块；framework 为底座，不得反向依赖业务 =====

    @ArchTest
    static final ArchRule framework_must_not_depend_on_business_modules =
            noClasses().that().resideInAPackage("com.finance.framework..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.finance.system..",
                            "com.finance.finance..",
                            "com.finance.contract..",
                            "com.finance.hr..",
                            "com.finance.workflow..",
                            "com.finance.admin..")
                    .allowEmptyShould(true);

    // ===== 硬约束 14：分层类命名规范 =====

    @ArchTest
    static final ArchRule controller_naming =
            classes().that().resideInAPackage("com.finance..controller..")
                    .should().haveSimpleNameEndingWith("Controller")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule service_naming =
            classes().that().resideInAPackage("com.finance..service..")
                    .should().haveSimpleNameEndingWith("Service")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule mapper_naming =
            classes().that().resideInAPackage("com.finance..mapper..")
                    .should().haveSimpleNameEndingWith("Mapper")
                    .allowEmptyShould(true);

    // ===== 硬约束 1：业务代码禁止 float/double（金额一律 BigDecimal） =====

    @ArchTest
    static final ArchRule business_fields_must_not_be_float_or_double =
            noFields()
                    .that().areDeclaredInClassesThat().resideInAPackage(
                            "com.finance.(system|finance|contract|hr|workflow)..")
                    .should().haveRawType(float.class)
                    .orShould().haveRawType(double.class)
                    .allowEmptyShould(true);
}
