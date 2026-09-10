package com.finance.system.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 分配入参（S2）：用户分配角色 roleIds / 角色分配菜单 menuIds。
 */
public class AssignDTO {

    @NotEmpty(message = "至少分配一个")
    private List<Long> ids;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}
