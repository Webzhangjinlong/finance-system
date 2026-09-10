package com.finance.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.system.domain.SysUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户-角色关联 Mapper（sys_user_role）。
 *
 * <p>先清后插必须物理删除（逻辑删残留行仍占用 uq_sys_user_role，L41 教训）。</p>
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /** 物理删除某用户的全部角色关联（分配时先清后插）。 */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int physicallyDeleteByUserId(@Param("userId") Long userId);
}
