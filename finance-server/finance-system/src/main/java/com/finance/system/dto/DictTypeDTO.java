package com.finance.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 字典类型 DTO（S3）。
 */
public class DictTypeDTO {

    private Long id;

    @NotBlank(message = "字典名称不能为空")
    @Size(max = 50, message = "字典名称最长 50")
    private String dictName;

    @NotBlank(message = "字典类型不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_]{1,49}$", message = "字典类型须以小写字母开头，仅小写字母/数字/下划线，2-50 位")
    private String dictType;

    @NotBlank(message = "状态不能为空")
    private String status;

    @Size(max = 200, message = "备注最长 200")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
