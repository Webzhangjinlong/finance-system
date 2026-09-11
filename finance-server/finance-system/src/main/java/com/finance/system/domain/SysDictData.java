package com.finance.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

/**
 * 字典数据（sys_dict_data，S3）。同 dict_type 下 dict_value 唯一（服务层校验）。
 */
@TableName("sys_dict_data")
public class SysDictData extends BaseEntity {

    /** 状态：正常。 */
    public static final String STATUS_ACTIVE = "ACTIVE";
    /** 状态：停用。 */
    public static final String STATUS_DISABLED = "DISABLED";

    private String dictType;
    private String dictLabel;
    private String dictValue;
    private Integer dictSort;
    private String status;

    public String getDictType() {
        return dictType;
    }

    public void setDictType(String dictType) {
        this.dictType = dictType;
    }

    public String getDictLabel() {
        return dictLabel;
    }

    public void setDictLabel(String dictLabel) {
        this.dictLabel = dictLabel;
    }

    public String getDictValue() {
        return dictValue;
    }

    public void setDictValue(String dictValue) {
        this.dictValue = dictValue;
    }

    public Integer getDictSort() {
        return dictSort;
    }

    public void setDictSort(Integer dictSort) {
        this.dictSort = dictSort;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
