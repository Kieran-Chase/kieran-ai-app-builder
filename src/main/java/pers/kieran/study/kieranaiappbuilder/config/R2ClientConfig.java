package pers.kieran.study.kieranaiappbuilder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * @author Kieran_Chase
 * @project kieran-ai-app-builder
 * @date 2026/7/17
 */

/**
 * Cloudflare R2 客户端配置类
 *
 * 对应教程中的 CosClientConfig
 */
@Configuration
@ConfigurationProperties(prefix = "r2.client")
@Data
public class R2ClientConfig {

    /**
     * 图片公开访问域名
     * 例如：https://img.wqc6.cc
     */
    private String host;

    /**
     * R2 的 S3 API 地址
     */
    private String endpoint;

    /**
     * Access Key ID
     */
    private String accessKeyId;

    /**
     * Secret Access Key
     */
    private String secretAccessKey;

    /**
     * R2 固定使用 auto
     */
    private String region;

    /**
     * 存储桶名称
     */
    private String bucket;

    /**
     * 创建 R2 的 S3 客户端
     */
    @Bean(name = "r2S3Client", destroyMethod = "close")
    public S3Client r2S3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                accessKeyId,
                secretAccessKey
        );

        S3Configuration s3Configuration = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false)
                .build();

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(
                        StaticCredentialsProvider.create(credentials)
                )
                .region(Region.of(region))
                .serviceConfiguration(s3Configuration)
                .build();
    }
}
