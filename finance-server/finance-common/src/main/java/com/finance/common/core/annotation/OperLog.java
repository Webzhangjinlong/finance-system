package com.finance.common.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解（S4，docs 5.5）：标注在 Controller 方法上，
 * 由 {@code OperLogAspect} 拦截落库（只增不删，硬约束 17 审计）。
 *
 * <p>示例：{@code @OperLog(title = "凭证审核", operType = OperType.AUDIT)}</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {

    /** 操作模块标题（如"凭证审核""合同审批"）。 */
    String title();

    /** 操作类型。 */
    OperType operType() default OperType.OTHER;
}
