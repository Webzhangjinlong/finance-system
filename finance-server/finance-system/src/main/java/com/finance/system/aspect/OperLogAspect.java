package com.finance.system.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.framework.security.SecurityUtils;
import com.finance.common.core.annotation.OperLog;
import com.finance.system.domain.SysOperLog;
import com.finance.system.mapper.SysOperLogMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 操作日志切面（S4，docs 5.5）：拦截 {@code @OperLog} 标注的 Controller 方法，
 * 记录 操作人/模块/URL/IP/参数（脱敏）/结果/耗时/状态 到 sys_oper_log（只增不删）。
 *
 * <p>硬约束 17：日志禁止输出密码/token/手机号/身份证等敏感信息 —— 敏感键值一律脱敏为
 * {@code ******}；参数与结果截断 2000 字符；切面自身失败仅 warn，不影响业务。</p>
 */
@Aspect
@Component
public class OperLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperLogAspect.class);
    private static final int MAX_LEN = 2000;
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "oldPassword", "newPassword", "token", "phone", "mobile",
            "idCard", "idcard", "secret", "authorization", "credential");

    private final SysOperLogMapper operLogMapper;
    private final ObjectMapper objectMapper;

    public OperLogAspect(SysOperLogMapper operLogMapper, ObjectMapper objectMapper) {
        this.operLogMapper = operLogMapper;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint pjp, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        Throwable error = null;
        Object result = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            try {
                saveLog(pjp, operLog, start, error, result);
            } catch (Exception e) {
                // 审计日志自身失败不阻断业务（与登录日志同策略，但会 warn 留痕）
                log.warn("写操作日志失败: {}", e.getMessage());
            }
        }
    }

    private void saveLog(ProceedingJoinPoint pjp, OperLog operLog, long start,
                         Throwable error, Object result) {
        SysOperLog oper = new SysOperLog();
        oper.setTitle(operLog.title());
        oper.setBusinessType(operLog.operType().name());
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        oper.setMethod(sig.getDeclaringTypeName() + "." + sig.getName());
        oper.setOperName(SecurityUtils.getUsername());

        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            oper.setRequestMethod(req.getMethod());
            oper.setOperUrl(req.getRequestURI());
            oper.setOperIp(extractIp(req));
        }

        oper.setOperParam(truncate(buildParamJson(pjp.getArgs()), MAX_LEN));
        if (error != null) {
            oper.setStatus(0);
            oper.setErrorMsg(truncate(error.getMessage(), MAX_LEN));
        } else {
            oper.setStatus(1);
            oper.setJsonResult(truncate(toJson(result), MAX_LEN));
        }
        oper.setCostTime(System.currentTimeMillis() - start);
        oper.setOperTime(OffsetDateTime.now());
        operLogMapper.insert(oper);
        log.info("操作日志：{} | {} | {} | {}ms | status={}",
                oper.getOperName(), oper.getTitle(), oper.getOperUrl(),
                oper.getCostTime(), oper.getStatus());
    }

    /** 构造请求参数 JSON：过滤 Servlet/MultipartFile 等不可序列化对象，敏感键脱敏。 */
    private String buildParamJson(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        List<Object> list = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null || arg instanceof ServletRequest || arg instanceof ServletResponse
                    || arg instanceof MultipartFile) {
                continue;
            }
            list.add(arg);
        }
        if (list.isEmpty()) {
            return null;
        }
        try {
            Object payload = list.size() == 1 ? list.get(0) : list;
            // 先转 JsonNode 再脱敏：能覆盖 POJO 字段（如 ResetPwdDTO.password），而不只是 Map/List
            Object node = maskSensitive(objectMapper.valueToTree(payload));
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return "[不可序列化参数: " + e.getClass().getSimpleName() + "]";
        }
    }

    /** 递归脱敏敏感键值（JsonNode 版：ObjectNode/ArrayNode 可遍历，POJO 已由 valueToTree 展开）。 */
    @SuppressWarnings("unchecked")
    private Object maskSensitive(Object value) {
        if (value instanceof com.fasterxml.jackson.databind.node.ObjectNode obj) {
            obj.fieldNames().forEachRemaining(name -> {
                if (SENSITIVE_KEYS.contains(name.toLowerCase())) {
                    obj.put(name, "******");
                } else {
                    maskSensitive(obj.get(name));
                }
            });
            return obj;
        }
        if (value instanceof com.fasterxml.jackson.databind.node.ArrayNode arr) {
            for (int i = 0; i < arr.size(); i++) {
                maskSensitive(arr.get(i));
            }
            return arr;
        }
        return value;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[不可序列化结果: " + e.getClass().getSimpleName() + "]";
        }
    }

    private String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "...(truncated)";
    }

    private String extractIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
