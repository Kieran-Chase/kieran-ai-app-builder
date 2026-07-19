package pers.kieran.study.kieranaiappbuilder.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pers.kieran.study.kieranaiappbuilder.config.R2ClientConfig;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author Kieran_Chase
 * @project kieran-ai-app-builder
 * @date 2026/7/17
 */

/**
 * Cloudflare R2 对象存储管理器
 *
 * 对应教程中的 CosManager
 */
@Component
@Slf4j
public class R2Manager {

    private final R2ClientConfig r2ClientConfig;

    private final S3Client s3Client;

    public R2Manager(
            R2ClientConfig r2ClientConfig,
            @Qualifier("r2S3Client") S3Client s3Client
    ) {
        this.r2ClientConfig = r2ClientConfig;
        this.s3Client = s3Client;
    }

    /**
     * 上传对象
     *
     * @param key  对象唯一键，例如 /screenshots/2026/07/17/test.jpg
     * @param file 本地文件
     * @return R2 上传结果
     */
    public PutObjectResponse putObject(String key, File file) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("对象 key 不能为空");
        }

        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("待上传文件不存在");
        }

        String normalizedKey = normalizeKey(key);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(r2ClientConfig.getBucket())
                .key(normalizedKey)
                .contentType(getContentType(file))
                .build();

        return s3Client.putObject(
                putObjectRequest,
                RequestBody.fromFile(file)
        );
    }

    /**
     * 上传文件到 R2 并返回公开访问 URL
     *
     * @param key  R2 对象键
     * @param file 要上传的文件
     * @return 文件公开访问 URL，失败返回 null
     */
    public String uploadFile(String key, File file) {
        try {
            String normalizedKey = normalizeKey(key);

            PutObjectResponse result = putObject(normalizedKey, file);

            if (result.sdkHttpResponse().isSuccessful()) {
                String url = buildPublicUrl(normalizedKey);

                log.info(
                        "文件上传 R2 成功: {} -> {}",
                        file.getName(),
                        url
                );

                return url;
            }

            log.error(
                    "文件上传 R2 失败，HTTP 状态码: {}",
                    result.sdkHttpResponse().statusCode()
            );
            return null;
        } catch (Exception e) {
            log.error(
                    "文件上传 R2 异常，key: {}, file: {}",
                    key,
                    file == null ? null : file.getAbsolutePath(),
                    e
            );
            return null;
        }
    }

    /**
     * 拼接图片公开访问地址
     */
    private String buildPublicUrl(String key) {
        String host = r2ClientConfig.getHost();

        if (host == null || host.isBlank()) {
            throw new IllegalStateException("未配置 r2.client.host");
        }

        // 删除域名结尾的斜杠
        host = host.replaceAll("/+$", "");

        return host + "/" + normalizeKey(key);
    }

    /**
     * R2 对象 key 不使用开头的 /
     *
     * 教程后面生成的 key 是：
     * /screenshots/2026/07/17/xxx.jpg
     *
     * 实际上传到 R2 时转换成：
     * screenshots/2026/07/17/xxx.jpg
     */
    private String normalizeKey(String key) {
        String result = key.trim();

        while (result.startsWith("/")) {
            result = result.substring(1);
        }

        return result;
    }

    /**
     * 识别文件的 Content-Type，
     * 避免浏览器把图片当普通二进制文件下载
     */
    private String getContentType(File file) {
        try {
            String contentType = Files.probeContentType(file.toPath());

            if (contentType != null && !contentType.isBlank()) {
                return contentType;
            }
        } catch (IOException e) {
            log.warn("无法识别文件类型: {}", file.getName(), e);
        }

        return "application/octet-stream";
    }
}
