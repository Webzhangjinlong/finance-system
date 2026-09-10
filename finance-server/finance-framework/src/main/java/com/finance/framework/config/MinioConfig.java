package com.finance.framework.config;

import com.finance.framework.storage.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端配置（W3 附件上传基础设施）。
 *
 * <p>启动时自检 bucket：不存在则自动创建（本地/生产首次部署免手工建桶）。</p>
 */
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Bean
    public MinioClient minioClient(MinioProperties props) {
        MinioClient client = MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
        ensureBucket(client, props.getBucket());
        return client;
    }

    private void ensureBucket(MinioClient client, String bucket) {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket 已创建：{}", bucket);
            } else {
                log.info("MinIO bucket 已存在：{}", bucket);
            }
        } catch (Exception e) {
            log.warn("MinIO bucket 检查失败（服务不可达？应用继续启动，上传时将再次报错）：{}", e.getMessage());
        }
    }
}
