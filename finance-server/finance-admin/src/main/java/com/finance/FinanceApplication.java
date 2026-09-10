package com.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 财务管理系统启动入口。
 *
 * <p>扫描根包 com.finance 下全部模块；Mapper 通过各 Mapper 接口上的 @Mapper 注解注册；
 * @EnableScheduling 启用定时任务（W4 到期提醒：ReminderTask 08:00/08:30）。</p>
 */
@SpringBootApplication(scanBasePackages = "com.finance")
@EnableScheduling
public class FinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceApplication.class, args);
    }
}
