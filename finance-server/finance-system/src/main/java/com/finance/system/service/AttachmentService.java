package com.finance.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.framework.storage.MinioProperties;
import com.finance.system.domain.SysAttachment;
import com.finance.system.mapper.SysAttachmentMapper;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 附件上传服务（W3，docs 7.2 附件管理）。
 *
 * <p>规则：类型白名单（图片/PDF/Office/文本）+ 魔数校验（防伪造扩展名）+ 大小限制 10MB；
 * 对象名 company_code/yyyyMM/uuid.ext（MinIO 路径前缀公司隔离，docs 2.4）；
 * 元数据落 sys_attachment（五件套 + 公司隔离）；下载按 id 从 MinIO 取流。
 * 病毒扫描：生产建议接入 ClamAV（本环境以类型白名单 + 魔数 + 大小做基础防护）。</p>
 */
@Service
public class AttachmentService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);

    /** 单文件大小上限：10MB。 */
    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    /** 扩展名 → 期望文件头魔数（十六进制，前缀匹配）。 */
    private static final Map<String, String[]> MAGIC_BY_EXT = new HashMap<>();

    static {
        MAGIC_BY_EXT.put("jpg", new String[]{"ffd8ff"});
        MAGIC_BY_EXT.put("jpeg", new String[]{"ffd8ff"});
        MAGIC_BY_EXT.put("png", new String[]{"89504e47"});
        MAGIC_BY_EXT.put("gif", new String[]{"47494638"});
        MAGIC_BY_EXT.put("webp", new String[]{"52494646"});
        MAGIC_BY_EXT.put("pdf", new String[]{"25504446"});
        MAGIC_BY_EXT.put("doc", new String[]{"d0cf11e0"});
        MAGIC_BY_EXT.put("xls", new String[]{"d0cf11e0"});
        MAGIC_BY_EXT.put("docx", new String[]{"504b0304"});
        MAGIC_BY_EXT.put("xlsx", new String[]{"504b0304"});
        MAGIC_BY_EXT.put("txt", new String[]{"efbbbf", "0d0a", "0a"});
        MAGIC_BY_EXT.put("csv", new String[]{"efbbbf", "0d0a", "0a"});
    }

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final SysAttachmentMapper attachmentMapper;

    public AttachmentService(MinioClient minioClient,
                             MinioProperties minioProperties,
                             SysAttachmentMapper attachmentMapper) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
        this.attachmentMapper = attachmentMapper;
    }

    /**
     * 上传：校验 → 存 MinIO → 落元数据 → 返回附件记录。
     *
     * @param companyCode 当前登录公司
     * @param file        待上传文件
     * @param bizType     业务类型（可空）
     * @param bizId       业务单号（可空）
     */
    public SysAttachment upload(String companyCode, MultipartFile file, String bizType, Long bizId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小超过上限 10MB");
        }
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename());
        String ext = extractExt(originalName);
        if (!MAGIC_BY_EXT.containsKey(ext)) {
            throw new BusinessException("不支持的文件类型：" + ext);
        }
        byte[] head = readHead(file);
        if (!magicMatches(ext, head)) {
            throw new BusinessException("文件内容与扩展名不符，疑似伪造或损坏");
        }

        String objectName = buildObjectName(companyCode, ext);
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                    .build());
        } catch (Exception e) {
            log.error("MinIO 上传失败：{}", e.getMessage(), e);
            throw new BusinessException("附件存储失败，请稍后重试");
        }

        SysAttachment attachment = new SysAttachment();
        attachment.setCompanyCode(companyCode);
        attachment.setFileName(originalName);
        attachment.setObjectName(objectName);
        attachment.setBucket(minioProperties.getBucket());
        attachment.setUrl(minioProperties.getEndpoint() + "/" + minioProperties.getBucket() + "/" + objectName);
        attachment.setFileSize(file.getSize());
        attachment.setContentType(file.getContentType());
        attachment.setBizType(bizType);
        attachment.setBizId(bizId);
        attachmentMapper.insert(attachment);
        log.info("附件上传成功：company={} object={} name={} size={}", companyCode, objectName, originalName, file.getSize());
        return attachment;
    }

    /** 按 id 查询（公司隔离）。 */
    public SysAttachment getAttachment(String companyCode, Long id) {
        SysAttachment attachment = attachmentMapper.selectById(id);
        if (attachment == null || attachment.getDeleted() != null && attachment.getDeleted() == 1
                || !companyCode.equals(attachment.getCompanyCode())) {
            throw new BusinessException("附件不存在");
        }
        return attachment;
    }

    /** 下载流（MinIO 取流；无访问权限场景走签名 URL 时另配）。 */
    public InputStream openStream(SysAttachment attachment) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(attachment.getBucket())
                    .object(attachment.getObjectName())
                    .build());
        } catch (Exception e) {
            log.error("MinIO 取流失败：{}", e.getMessage(), e);
            throw new BusinessException("附件读取失败");
        }
    }

    /** 对象是否存在。 */
    public boolean objectExists(String bucket, String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder().bucket(bucket).object(objectName).build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 分页列表（公司隔离，按上传时间倒序）。 */
    public PageResult<SysAttachment> page(String companyCode, long page, long size) {
        Page<SysAttachment> p = attachmentMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<SysAttachment>()
                        .eq(SysAttachment::getCompanyCode, companyCode)
                        .orderByDesc(SysAttachment::getCreateTime));
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    // ==================== 内部实现 ====================

    private String extractExt(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private byte[] readHead(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] head = new byte[8];
            int n = in.read(head);
            byte[] result = new byte[Math.max(n, 0)];
            System.arraycopy(head, 0, result, 0, result.length);
            return result;
        } catch (Exception e) {
            throw new BusinessException("文件读取失败");
        }
    }

    private boolean magicMatches(String ext, byte[] head) {
        if (head.length == 0) {
            return false;
        }
        String hex = toHex(head);
        for (String magic : MAGIC_BY_EXT.get(ext)) {
            if (hex.startsWith(magic)) {
                return true;
            }
        }
        return false;
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String buildObjectName(String companyCode, String ext) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        return companyCode + "/" + date + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
    }
}
