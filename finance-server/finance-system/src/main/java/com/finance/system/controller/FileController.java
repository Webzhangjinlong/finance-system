package com.finance.system.controller;

import com.finance.common.core.Result;
import com.finance.common.core.domain.PageResult;
import com.finance.framework.security.SecurityUtils;
import com.finance.system.domain.SysAttachment;
import com.finance.system.service.AttachmentService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 公共附件接口（W3，docs 7.2）：/common/upload、/common/file、/common/attachment。
 *
 * <p>上传（类型/大小/魔数校验 → MinIO + sys_attachment 元数据）、按 id 下载、
 * 分页列表（公司隔离）。权限：上传/下载需 system:attachment:upload/download，
 * 列表需 system:attachment:list；凭证/报销/合同可传 bizType/bizId 复用。</p>
 */
@RestController
@RequestMapping("/common")
public class FileController {

    private final AttachmentService attachmentService;

    public FileController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    /** 上传附件（multipart/form-data：file + 可选 bizType/bizId）。 */
    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('system:attachment:upload')")
    public Result<SysAttachment> upload(@RequestParam("file") MultipartFile file,
                                        @RequestParam(required = false) String bizType,
                                        @RequestParam(required = false) Long bizId) {
        return Result.ok(attachmentService.upload(
                SecurityUtils.getCompanyCode(), file, bizType, bizId));
    }

    /** 下载附件（MinIO 取流，按 id + 公司隔离）。 */
    @GetMapping("/file/{id}")
    @PreAuthorize("hasAuthority('system:attachment:download')")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id) {
        SysAttachment attachment = attachmentService.getAttachment(SecurityUtils.getCompanyCode(), id);
        InputStream stream = attachmentService.openStream(attachment);
        String encodedName = URLEncoder.encode(attachment.getFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(stream));
    }

    /** 附件分页列表（公司隔离，上传时间倒序）。 */
    @GetMapping("/attachment/list")
    @PreAuthorize("hasAuthority('system:attachment:list')")
    public Result<PageResult<SysAttachment>> list(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size) {
        return Result.ok(attachmentService.page(SecurityUtils.getCompanyCode(), page, size));
    }
}
