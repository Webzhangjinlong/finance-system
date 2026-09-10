package com.finance.system.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

import java.util.List;

/**
 * 菜单/权限点（sys_menu）。菜单类型：DIR 目录 / MENU 菜单 / BUTTON 按钮。
 */
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    /** 菜单类型：目录。 */
    public static final String TYPE_DIR = "DIR";
    /** 菜单类型：菜单。 */
    public static final String TYPE_MENU = "MENU";
    /** 菜单类型：按钮。 */
    public static final String TYPE_BUTTON = "BUTTON";
    /** 菜单状态：正常。 */
    public static final String STATUS_ACTIVE = "ACTIVE";
    /** 菜单状态：停用。 */
    public static final String STATUS_DISABLED = "DISABLED";

    private Long parentId;
    private String menuName;
    private String menuType;
    private String path;
    private String component;
    private String perms;
    private String icon;
    private Integer sortOrder;
    private String status;

    /** 子菜单（非表字段，树接口装配）。 */
    @TableField(exist = false)
    private List<SysMenu> children;

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getMenuType() {
        return menuType;
    }

    public void setMenuType(String menuType) {
        this.menuType = menuType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getPerms() {
        return perms;
    }

    public void setPerms(String perms) {
        this.perms = perms;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<SysMenu> getChildren() {
        return children;
    }

    public void setChildren(List<SysMenu> children) {
        this.children = children;
    }
}
