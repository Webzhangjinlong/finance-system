package com.finance.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 角色创建/编辑入参（S2）。
 */
public class RoleDTO {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称过长")
    private String roleName;

    @NotBlank(message = "角色标识不能为空")
    @Size(max = 64, message = "角色标识过长")
    private String roleKey;

    private Integer roleSort;

    private String status;

    @Size(max = 255, message = "备注过长")
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
}
