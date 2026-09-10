package com.finance.system.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

import java.util.List;

/**
 * 角色（sys_role）。
 */
@TableName("sys_role")
public class SysRole extends BaseEntity {

    /** 角色状态：正常。 */
    public static final String STATUS_ACTIVE = "ACTIVE";
    /** 角色状态：停用。 */
    public static final String STATUS_DISABLED = "DISABLED";

    private String roleName;
    private String roleKey;
    private Integer roleSort;
    private String status;
    private String remark;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleKey() {
        return roleKey;
    }

    public void setRoleKey(String roleKey) {
        this.roleKey = roleKey;
    }

    public Integer getRoleSort() {
        return roleSort;
    }

    public void setRoleSort(Integer roleSort) {
        this.roleSort = roleSort;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /** 角色拥有的菜单 ID（非表字段，详情装配）。 */
    @TableField(exist = false)
    private List<Long> menuIds;

    public List<Long> getMenuIds() {
        return menuIds;
    }

    public void setMenuIds(List<Long> menuIds) {
        this.menuIds = menuIds;
    }
}
