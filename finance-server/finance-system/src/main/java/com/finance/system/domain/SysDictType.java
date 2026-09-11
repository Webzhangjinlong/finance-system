package com.finance.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

/**
 * 字典类型（sys_dict_type，S3）。全局字典（不按公司隔离），dict_type 唯一。
 */
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {

    /** 状态：正常。 */
    public static final String STATUS_ACTIVE = "ACTIVE";
    /** 状态：停用。 */
    public static final String STATUS_DISABLED = "DISABLED";

    private String dictName;
    private String dictType;
    private String status;
    private String remark;

    public String getDictName() {
        return dictName;
    }

    public void setDictName(String dictName) {
        this.dictName = dictName;
    }

    public String getDictType() {
        return dictType;
    }

    public void setDictType(String dictType) {
        this.dictType = dictType;
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
