package com.finance.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 字典数据 DTO（S3）。
 */
public class DictDataDTO {

    private Long id;

    @NotBlank(message = "字典类型不能为空")
    private String dictType;

    @NotBlank(message = "字典标签不能为空")
    @Size(max = 50, message = "字典标签最长 50")
    private String dictLabel;

    @NotBlank(message = "字典键值不能为空")
    @Size(max = 50, message = "字典键值最长 50")
    private String dictValue;

    @NotNull(message = "排序不能为空")
    private Integer dictSort;

    @NotBlank(message = "状态不能为空")
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
