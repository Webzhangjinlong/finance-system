package com.finance.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 合同提交审批入参（C2，docs 5.2）。
 *
 * <p>一期单人顺序审批：approver 为当前审批人（按金额分级审批留待二期，
 * 分级规则见 docs 5.2——≤10万止步部门+财务 / 10-100万加分管领导 / &gt;100万加总经理）。</p>
 */
public class ContractSubmitDTO {

    @NotBlank(message = "审批人不能为空")
    @Size(max = 64, message = "审批人最长 64 字符")
    private String approver;

    @Size(max = 255, message = "审批意见最长 255 字符")
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
