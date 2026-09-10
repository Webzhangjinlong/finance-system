package com.finance.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 发起流程入参（W1）。 */
public class WorkflowStartDTO {

    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    @NotNull(message = "业务单号不能为空")
    private Long businessId;

    @NotBlank(message = "审批人不能为空")
    private String approver;

    /** 审批意见（可选，携带业务备注）。 */
    private String comment;

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public String getApprover() {
        return approver;
    }

    public void setApprover(String approver) {
        this.approver = approver;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
