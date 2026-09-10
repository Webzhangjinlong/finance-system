package com.finance.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 新建/编辑报销单入参（F6）。
 */
public class ExpenseClaimDTO {

    /** 费用类型：TRAVEL/MEAL/OFFICE/OTHER。 */
    @NotBlank(message = "费用类型不能为空")
    @Size(max = 32, message = "费用类型过长")
    private String expenseType;

    @NotBlank(message = "事由不能为空")
    @Size(max = 255, message = "事由过长")
    private String reason;

    @Size(max = 64, message = "发票号过长")
    private String invoiceNo;

    @NotEmpty(message = "至少一条报销明细")
    @Valid
    private List<ExpenseItemDTO> items;

    public String getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(String expenseType) {
        this.expenseType = expenseType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public List<ExpenseItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ExpenseItemDTO> items) {
        this.items = items;
    }
}
