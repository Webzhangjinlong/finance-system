package com.finance.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.system.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;

/**
 * 用户 Mapper（sys_user）。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 查询用户拥有的角色 key 集合（user → role）。 */
    @Select("""
            SELECT r.role_key
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
              AND r.status = 'ACTIVE'
              AND r.deleted = 0
              AND ur.deleted = 0
            """)
    java.util.List<String> selectRoleKeysByUserId(@Param("userId") Long userId);

    /** 查询用户拥有的权限码集合（user → role → menu.perms，非空权限码）。 */
    @Select("""
            SELECT DISTINCT m.perms
            FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.id
            JOIN sys_user_role ur ON ur.role_id = rm.role_id
            WHERE ur.user_id = #{userId}
              AND m.status = 'ACTIVE'
              AND m.deleted = 0
              AND rm.deleted = 0
              AND ur.deleted = 0
              AND m.perms IS NOT NULL
              AND m.perms <> ''
            """)
    java.util.List<String> selectPermsByUserId(@Param("userId") Long userId);

    /** 更新最后登录信息。 */
    @Update("UPDATE sys_user SET login_ip = #{ip}, login_date = #{loginDate} WHERE id = #{userId}")
    int updateLoginInfo(@Param("userId") Long userId,
                        @Param("ip") String ip,
                        @Param("loginDate") OffsetDateTime loginDate);
}
