package com.finance.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.system.domain.SysRoleMenu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色-菜单关联 Mapper（sys_role_menu）。
 *
 * <p>先清后插必须物理删除（逻辑删残留行仍占用 uq_sys_role_menu，L41 教训）。</p>
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    /** 物理删除某角色的全部菜单关联（分配时先清后插）。 */
    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int physicallyDeleteByRoleId(@Param("roleId") Long roleId);

    /** 物理删除某菜单的全部角色关联（删除菜单时清理）。 */
    @Delete("DELETE FROM sys_role_menu WHERE menu_id = #{menuId}")
    int physicallyDeleteByMenuId(@Param("menuId") Long menuId);
}
