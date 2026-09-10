package com.finance.framework.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.finance.framework.security.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.OffsetDateTime;

/**
 * MyBatis-Plus 配置：分页插件、公共字段自动填充。
 *
 * <p>create_by/update_by 自动取当前登录用户（未登录为 null）；
 * 公司隔离拦截器（多公司数据权限）将在数据权限 Gate 接入，届时所有 SQL 自动追加 company_code 过滤。</p>
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", OffsetDateTime.class, OffsetDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", OffsetDateTime.class, OffsetDateTime.now());
                this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
                String username = SecurityUtils.getUsername();
                if (username != null) {
                    this.strictInsertFill(metaObject, "createBy", String.class, username);
                    this.strictInsertFill(metaObject, "updateBy", String.class, username);
                }
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", OffsetDateTime.class, OffsetDateTime.now());
                String username = SecurityUtils.getUsername();
                if (username != null) {
                    this.strictUpdateFill(metaObject, "updateBy", String.class, username);
                }
            }
        };
    }
}
