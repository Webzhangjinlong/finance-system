package com.finance.system;

import com.finance.common.core.exception.BusinessException;
import com.finance.framework.storage.MinioProperties;
import com.finance.system.domain.SysAttachment;
import com.finance.system.mapper.SysAttachmentMapper;
import com.finance.system.service.AttachmentService;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 附件上传服务单测（W3，docs 7.2）：类型白名单 / 大小上限 / 魔数防伪 / 成功上传落 MinIO+元数据。
 *
 * <p>纯 Mockito（不启动 Spring / 不依赖真实 MinIO）：MinioClient.putObject 打桩成功路径。
 * 真实 MinIO 联调走浏览器实测与手工冒烟。</p>
 */
class AttachmentServiceTest {

    private MinioClient minioClient;
    private MinioProperties props;
    private SysAttachmentMapper mapper;
    private AttachmentService service;

    @BeforeEach
    void setUp() {
        minioClient = mock(MinioClient.class);
        props = new MinioProperties();
        mapper = mock(SysAttachmentMapper.class);
        service = new AttachmentService(minioClient, props, mapper);
    }

    @Test
    void upload_validPng_success_storesObjectAndMeta() throws Exception {
        byte[] png = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "报销单.png", "image/png", png);

        SysAttachment a = service.upload("DEMO", file, "EXPENSE", 12345L);

        assertThat(a.getFileName()).isEqualTo("报销单.png");
        assertThat(a.getObjectName()).startsWith("DEMO/").endsWith(".png");
        assertThat(a.getBucket()).isEqualTo("finance");
        assertThat(a.getCompanyCode()).isEqualTo("DEMO");
        assertThat(a.getBizType()).isEqualTo("EXPENSE");
        assertThat(a.getBizId()).isEqualTo(12345L);
        assertThat(a.getUrl()).contains("localhost:9000");
    }

    @Test
    void upload_disallowedExtension_rejected() {
        MockMultipartFile file = new MockMultipartFile("file", "木马.exe", "application/octet-stream",
                new byte[]{0x4D, 0x5A});
        assertThatThrownBy(() -> service.upload("DEMO", file, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的文件类型");
    }

    @Test
    void upload_sizeTooLarge_rejected() {
        org.springframework.web.multipart.MultipartFile file = mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(AttachmentService.MAX_FILE_SIZE + 1);
        assertThatThrownBy(() -> service.upload("DEMO", file, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超过上限");
    }

    @Test
    void upload_fakeExtension_magicMismatch_rejected() {
        // 扩展名 png 但内容为纯文本 → 魔数不匹配（防伪造扩展名）
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png",
                "hello this is not a png".getBytes());
        assertThatThrownBy(() -> service.upload("DEMO", file, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("内容与扩展名不符");
    }

    @Test
    void upload_emptyFile_rejected() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> service.upload("DEMO", file, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("为空");
    }
}
