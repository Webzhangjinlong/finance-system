package com.finance.finance.controller;

import com.finance.common.core.Result;
import com.finance.common.core.exception.BusinessException;
import com.finance.finance.dto.BookRow;
import com.finance.finance.service.BookService;
import com.finance.framework.security.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 账簿查询接口（F3，docs 4.3）：/finance/book，权限 finance:book:list。
 *
 * <p>type=ledger（总账）/ detail（明细账）/ journal（日记账）；数据来源已过账凭证。</p>
 */
@RestController
@RequestMapping("/finance/book")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /** 账簿查询（总账/明细账/日记账）。 */
    @GetMapping("/{type}")
    @PreAuthorize("hasAuthority('finance:book:list')")
    public Result<List<BookRow>> query(@PathVariable String type,
                                       @RequestParam int periodYear,
                                       @RequestParam int periodMonth,
                                       @RequestParam(required = false) Long subjectId) {
        String companyCode = SecurityUtils.getCompanyCode();
        List<BookRow> rows = switch (type) {
            case "ledger" -> bookService.ledger(companyCode, periodYear, periodMonth, subjectId);
            case "detail" -> bookService.detail(companyCode, periodYear, periodMonth, subjectId);
            case "journal" -> bookService.journal(companyCode, periodYear, periodMonth, subjectId);
            default -> throw new BusinessException("不支持的账簿类型：" + type);
        };
        return Result.ok(rows);
    }

    /** 账簿 Excel 导出。 */
    @GetMapping("/{type}/export")
    @PreAuthorize("hasAuthority('finance:book:list')")
    public void export(@PathVariable String type,
                       @RequestParam int periodYear,
                       @RequestParam int periodMonth,
                       @RequestParam(required = false) Long subjectId,
                       HttpServletResponse response) {
        bookService.export(SecurityUtils.getCompanyCode(), type,
                periodYear, periodMonth, subjectId, response);
    }
}
