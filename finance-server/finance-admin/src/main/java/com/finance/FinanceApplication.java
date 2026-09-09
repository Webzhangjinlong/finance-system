package com.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 财务管理系统启动入口。
 *
 * <p>扫描根包 com.finance 下全部模块；Mapper 通过各 Mapper 接口上的 @Mapper 注解注册。</p>
 */
@SpringBootApplication(scanBasePackages = "com.finance")
public class FinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceApplication.class, args);
    }
}
