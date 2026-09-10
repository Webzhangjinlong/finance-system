package com.finance.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 报销单提交审批入参（F6，复用 W1 单人顺序审批）：指定审批人 + 意见。
 */
public class ExpenseSubmitDTO {

    @NotBlank(message = "审批人不能为空")
    @Size(max = 64, message = "审批人过长")
    private String approver;

    @Size(max = 500, message = "意见过长")
    private String comment;

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
