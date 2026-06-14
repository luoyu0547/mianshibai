package com.mianshiba.ai.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.mianshiba.ai.config.FileUploadProperties;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.model.vo.FileUploadVO;
import com.mianshiba.ai.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 本地文件服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileServiceImpl implements FileService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final String AVATAR_DIR = "avatar";

    private final FileUploadProperties fileUploadProperties;

    @Override
    public FileUploadVO uploadAvatar(MultipartFile file) {
        // 1. 校验上传文件是否合法
        validateAvatarFile(file);

        // 2. 根据配置选择本地或 OSS 存储
        if ("aliyun".equalsIgnoreCase(fileUploadProperties.getProvider())) {
            return uploadAvatarToAliyunOss(file);
        }

        // 3. 保存到本地存储
        return uploadAvatarToLocal(file);
    }

    private FileUploadVO uploadAvatarToLocal(MultipartFile file) {
        // 1. 创建头像文件存储路径
        String extension = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + extension;
        Path targetDir = Paths.get(fileUploadProperties.getUploadDir(), AVATAR_DIR).toAbsolutePath().normalize();
        Path targetFile = targetDir.resolve(filename).normalize();

        // 2. 保存文件并返回公开访问地址
        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile);
        } catch (IOException ex) {
            log.error("upload_avatar_failed filename={}", file.getOriginalFilename(), ex);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传失败");
        }

        return FileUploadVO.builder()
                .url(buildLocalPublicUrl(filename))
                .originalName(file.getOriginalFilename())
                .size(file.getSize())
                .build();
    }

    private FileUploadVO uploadAvatarToAliyunOss(MultipartFile file) {
        // 1. 校验 OSS 配置是否完整
        FileUploadProperties.Aliyun aliyun = fileUploadProperties.getAliyun();
        validateAliyunConfig(aliyun);

        // 2. 生成 OSS 对象路径
        String extension = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + extension;
        String objectName = AVATAR_DIR + "/" + filename;

        // 3. 上传文件到 OSS
        OSS ossClient = new OSSClientBuilder().build(
                aliyun.getEndpoint(),
                aliyun.getAccessKeyId(),
                aliyun.getAccessKeySecret());
        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(aliyun.getBucket(), objectName, inputStream);
        } catch (IOException ex) {
            log.error("upload_avatar_to_oss_failed filename={}", file.getOriginalFilename(), ex);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传失败");
        } finally {
            ossClient.shutdown();
        }

        return FileUploadVO.builder()
                .url(buildAliyunPublicUrl(aliyun, objectName))
                .originalName(file.getOriginalFilename())
                .size(file.getSize())
                .build();
    }

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择头像文件");
        }
        if (file.getSize() > fileUploadProperties.getMaxSize()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像不能超过 2MB");
        }
        String contentType = file.getContentType();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 JPG、PNG、WebP 图片");
        }
        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片后缀不合法");
        }
    }

    private String getExtension(String originalFilename) {
        String filename = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片后缀不能为空");
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private void validateAliyunConfig(FileUploadProperties.Aliyun aliyun) {
        if (!StringUtils.hasText(aliyun.getEndpoint())
                || !StringUtils.hasText(aliyun.getBucket())
                || !StringUtils.hasText(aliyun.getAccessKeyId())
                || !StringUtils.hasText(aliyun.getAccessKeySecret())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "OSS 配置不完整");
        }
    }

    private String buildLocalPublicUrl(String filename) {
        String prefix = fileUploadProperties.getPublicPrefix();
        String normalizedPrefix = prefix.startsWith("/") ? prefix : "/" + prefix;
        return normalizedPrefix + "/" + AVATAR_DIR + "/" + filename;
    }

    private String buildAliyunPublicUrl(FileUploadProperties.Aliyun aliyun, String objectName) {
        if (StringUtils.hasText(aliyun.getDomain())) {
            return trimTrailingSlash(aliyun.getDomain()) + "/" + objectName;
        }
        String endpoint = aliyun.getEndpoint()
                .replace("https://", "")
                .replace("http://", "");
        return "https://" + aliyun.getBucket() + "." + endpoint + "/" + objectName;
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
