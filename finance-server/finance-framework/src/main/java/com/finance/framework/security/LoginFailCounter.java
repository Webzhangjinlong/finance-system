package com.finance.framework.security;

/**
 * 登录失败计数与锁定（硬约束 21：登录失败 5 次锁定 15 分钟）。
 *
 * <p>接口化以便测试注入内存/模拟实现；默认实现走 Redis。</p>
 */
public interface LoginFailCounter {

    /** 记录一次失败，返回当前累计失败次数。 */
    long recordFailure(String username);

    /** 清除失败记录（登录成功后调用）。 */
    void clearFailure(String username);

    /** 该用户是否处于锁定状态。 */
    boolean isLocked(String username);

    /** 锁定剩余秒数；未锁定返回 0。 */
    long lockRemainingSeconds(String username);

    /** 失败次数上限提示文案（默认 5 次）。 */
    default String maxFailTimesHint() {
        return "5";
    }
}
