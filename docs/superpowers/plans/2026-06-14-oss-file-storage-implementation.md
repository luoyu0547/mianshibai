# OSS File Storage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add OSS-backed file storage for existing avatar uploads and opt-in interview answer recordings.

**Architecture:** Introduce a storage-provider abstraction below the existing business file service. Avatars remain public URL uploads, while interview audio stores only object metadata and exposes playback through authenticated short-lived URLs.

**Tech Stack:** Spring Boot 3.5, Java 17, MyBatis-Plus, Aliyun OSS SDK, Maven, Vue 3, TypeScript, Pinia, Vite, Element Plus.

---

## Current Worktree Notice

The worktree already contains uncommitted and untracked file-upload-related files, including `FileUploadProperties.java`, `FileController.java`, `FileUploadVO.java`, `FileService.java`, and `LocalFileServiceImpl.java`. Treat those as existing user work: read and modify them only as needed for this feature, and do not revert unrelated changes.

Do not run `git reset`, `git checkout --`, or any destructive command. Do not commit unless the user explicitly asks for a commit.

## File Structure

Backend files to modify or create:

- Modify: `backend/pom.xml` only if Aliyun OSS dependency is missing; current workspace already includes `com.aliyun.oss:aliyun-sdk-oss`.
- Modify: `backend/src/main/resources/application.yml` to add signed URL and file-size settings.
- Modify: `backend/src/main/resources/sql/init.sql` to add interview audio metadata columns.
- Modify: `backend/src/main/java/com/mianshiba/ai/config/FileUploadProperties.java` for nested avatar/audio settings and signed URL expiration.
- Create: `backend/src/main/java/com/mianshiba/ai/service/storage/FileStorageService.java` for provider-neutral upload and URL operations.
- Create: `backend/src/main/java/com/mianshiba/ai/service/storage/StoredFile.java` for uploaded object metadata.
- Create: `backend/src/main/java/com/mianshiba/ai/service/storage/UploadFileCommand.java` for upload input metadata.
- Create: `backend/src/main/java/com/mianshiba/ai/service/storage/impl/LocalFileStorageService.java` for local provider.
- Create: `backend/src/main/java/com/mianshiba/ai/service/storage/impl/AliyunOssFileStorageService.java` for OSS provider.
- Modify: `backend/src/main/java/com/mianshiba/ai/service/FileService.java` to expose avatar upload, audio upload, and signed URL methods.
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/LocalFileServiceImpl.java` by renaming or refactoring it into business-level `FileServiceImpl`.
- Modify: `backend/src/main/java/com/mianshiba/ai/controller/FileController.java` to keep avatar behavior stable.
- Modify: `backend/src/main/java/com/mianshiba/ai/model/vo/FileUploadVO.java` to include `objectKey` and `contentType` if useful for internal reuse.
- Modify: `backend/src/main/java/com/mianshiba/ai/model/entity/InterviewTurn.java` to add audio metadata fields.
- Modify: `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewTurnVO.java` to expose `hasAnswerAudio` and audio metadata safe fields.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewAudioUploadVO.java` for audio upload response.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewAudioUrlVO.java` for signed playback URL response.
- Modify: `backend/src/main/java/com/mianshiba/ai/service/InterviewService.java` to add audio methods.
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/InterviewServiceImpl.java` to implement audio owner checks and metadata persistence.
- Modify: `backend/src/main/java/com/mianshiba/ai/controller/InterviewController.java` to add upload and playback URL endpoints.

Backend tests:

- Create: `backend/src/test/java/com/mianshiba/ai/service/storage/impl/LocalFileStorageServiceTest.java`.
- Create: `backend/src/test/java/com/mianshiba/ai/service/impl/FileServiceImplTest.java`.
- Modify: `backend/src/test/java/com/mianshiba/ai/service/impl/InterviewServiceImplTest.java`.
- Modify or create SQL tests under `backend/src/test/java/com/mianshiba/ai/sql/` to assert new `interview_turn` columns exist.

Frontend files to modify:

- Modify: `frontend/src/types/interview.ts` to add audio metadata types.
- Modify: `frontend/src/api/interview.ts` to add upload and signed URL API functions.
- Modify: `frontend/src/stores/interview.ts` to wrap the new API calls if the store pattern requires it.
- Modify: `frontend/src/views/interview/InterviewRoomPage.vue` to add opt-in recording persistence.
- Modify: `frontend/src/views/interview/InterviewReportPage.vue` to add playback controls.

## Task 1: Backend Storage Contracts And Configuration

**Files:**

- Create: `backend/src/main/java/com/mianshiba/ai/service/storage/UploadFileCommand.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/storage/StoredFile.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/storage/FileStorageService.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/config/FileUploadProperties.java`
- Modify: `backend/src/main/resources/application.yml`

- [ ] **Step 1: Add failing configuration binding test**

Create `backend/src/test/java/com/mianshiba/ai/config/FileUploadPropertiesTest.java`:

```java
package com.mianshiba.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class FileUploadPropertiesTest {

    @Test
    void applicationConfigBindsFileStorageProperties() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        environment.getPropertySources().addLast(loader.load(
                "application", new ClassPathResource("application.yml")).get(0));

        FileUploadProperties properties = Binder.get(environment)
                .bind("app.file", Bindable.of(FileUploadProperties.class))
                .orElseThrow();

        assertThat(properties.getProvider()).isEqualTo("local");
        assertThat(properties.getUploadDir()).isEqualTo("uploads");
        assertThat(properties.getPublicPrefix()).isEqualTo("/uploads");
        assertThat(properties.getSignedUrlExpiration()).isEqualTo(Duration.ofMinutes(10));
        assertThat(properties.getAvatar().getMaxSize()).isEqualTo(2 * 1024 * 1024L);
        assertThat(properties.getAudio().getMaxSize()).isEqualTo(20 * 1024 * 1024L);
    }
}
```

- [ ] **Step 2: Run the failing test**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=FileUploadPropertiesTest
```

Expected: FAIL because `signedUrlExpiration`, `avatar`, and `audio` properties do not exist yet.

- [ ] **Step 3: Add storage DTOs and interface**

Create `backend/src/main/java/com/mianshiba/ai/service/storage/UploadFileCommand.java`:

```java
package com.mianshiba.ai.service.storage;

import java.io.InputStream;

public record UploadFileCommand(
        InputStream inputStream,
        String objectKey,
        String originalName,
        String contentType,
        long size) {
}
```

Create `backend/src/main/java/com/mianshiba/ai/service/storage/StoredFile.java`:

```java
package com.mianshiba.ai.service.storage;

public record StoredFile(
        String objectKey,
        String url,
        String originalName,
        String contentType,
        long size) {
}
```

Create `backend/src/main/java/com/mianshiba/ai/service/storage/FileStorageService.java`:

```java
package com.mianshiba.ai.service.storage;

import java.time.Duration;

public interface FileStorageService {

    StoredFile upload(UploadFileCommand command);

    String getPublicUrl(String objectKey);

    String generateSignedUrl(String objectKey, Duration expiration);
}
```

- [ ] **Step 4: Extend file properties**

Update `backend/src/main/java/com/mianshiba/ai/config/FileUploadProperties.java` so it contains these fields while preserving existing Aliyun fields:

```java
package com.mianshiba.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "app.file")
public class FileUploadProperties {

    private String provider = "local";

    private String uploadDir = "uploads";

    private String publicPrefix = "/uploads";

    private long maxSize = 2 * 1024 * 1024;

    private Duration signedUrlExpiration = Duration.ofMinutes(10);

    private FileLimit avatar = new FileLimit(2 * 1024 * 1024L);

    private FileLimit audio = new FileLimit(20 * 1024 * 1024L);

    private Aliyun aliyun = new Aliyun();

    @Data
    public static class FileLimit {
        private long maxSize;

        public FileLimit() {
        }

        public FileLimit(long maxSize) {
            this.maxSize = maxSize;
        }
    }

    @Data
    public static class Aliyun {
        private String endpoint = "";
        private String bucket = "";
        private String accessKeyId = "";
        private String accessKeySecret = "";
        private String domain = "";
    }
}
```

Update `backend/src/main/resources/application.yml` under `app.file`:

```yaml
    signed-url-expiration: ${FILE_SIGNED_URL_EXPIRATION:PT10M}
    avatar:
      max-size: ${FILE_AVATAR_MAX_SIZE:2097152}
    audio:
      max-size: ${FILE_AUDIO_MAX_SIZE:20971520}
```

- [ ] **Step 5: Run the configuration test**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=FileUploadPropertiesTest
```

Expected: PASS.

## Task 2: Local And Aliyun Storage Providers

**Files:**

- Create: `backend/src/main/java/com/mianshiba/ai/service/storage/impl/LocalFileStorageService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/storage/impl/AliyunOssFileStorageService.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/storage/impl/LocalFileStorageServiceTest.java`

- [ ] **Step 1: Write local storage tests**

Create `backend/src/test/java/com/mianshiba/ai/service/storage/impl/LocalFileStorageServiceTest.java`:

```java
package com.mianshiba.ai.service.storage.impl;

import com.mianshiba.ai.config.FileUploadProperties;
import com.mianshiba.ai.service.storage.StoredFile;
import com.mianshiba.ai.service.storage.UploadFileCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void uploadWritesFileAndReturnsPublicUrl() throws Exception {
        FileUploadProperties properties = new FileUploadProperties();
        properties.setUploadDir(tempDir.toString());
        properties.setPublicPrefix("/uploads");
        LocalFileStorageService service = new LocalFileStorageService(properties);

        StoredFile stored = service.upload(new UploadFileCommand(
                new ByteArrayInputStream("abc".getBytes()),
                "avatar/user-1/test.txt",
                "test.txt",
                "text/plain",
                3));

        assertThat(Files.readString(tempDir.resolve("avatar/user-1/test.txt"))).isEqualTo("abc");
        assertThat(stored.objectKey()).isEqualTo("avatar/user-1/test.txt");
        assertThat(stored.url()).isEqualTo("/uploads/avatar/user-1/test.txt");
        assertThat(stored.originalName()).isEqualTo("test.txt");
        assertThat(stored.contentType()).isEqualTo("text/plain");
        assertThat(stored.size()).isEqualTo(3);
    }

    @Test
    void generateSignedUrlReturnsLocalPublicUrlForDevelopment() {
        FileUploadProperties properties = new FileUploadProperties();
        properties.setUploadDir(tempDir.toString());
        properties.setPublicPrefix("uploads");
        LocalFileStorageService service = new LocalFileStorageService(properties);

        assertThat(service.generateSignedUrl("audio/1/a.webm", Duration.ofMinutes(10)))
                .isEqualTo("/uploads/audio/1/a.webm");
    }
}
```

- [ ] **Step 2: Run local storage tests to verify failure**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=LocalFileStorageServiceTest
```

Expected: FAIL because `LocalFileStorageService` does not exist.

- [ ] **Step 3: Implement local provider**

Create `backend/src/main/java/com/mianshiba/ai/service/storage/impl/LocalFileStorageService.java`:

```java
package com.mianshiba.ai.service.storage.impl;

import com.mianshiba.ai.config.FileUploadProperties;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.service.storage.FileStorageService;
import com.mianshiba.ai.service.storage.StoredFile;
import com.mianshiba.ai.service.storage.UploadFileCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.file", name = "provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final FileUploadProperties fileUploadProperties;

    @Override
    public StoredFile upload(UploadFileCommand command) {
        Path rootDir = Paths.get(fileUploadProperties.getUploadDir()).toAbsolutePath().normalize();
        Path targetFile = rootDir.resolve(command.objectKey()).normalize();
        if (!targetFile.startsWith(rootDir)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件路径不合法");
        }
        try {
            Files.createDirectories(targetFile.getParent());
            Files.copy(command.inputStream(), targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.error("local_file_upload_failed objectKey={}", command.objectKey(), ex);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }
        return new StoredFile(command.objectKey(), getPublicUrl(command.objectKey()),
                command.originalName(), command.contentType(), command.size());
    }

    @Override
    public String getPublicUrl(String objectKey) {
        String prefix = fileUploadProperties.getPublicPrefix();
        String normalizedPrefix = prefix.startsWith("/") ? prefix : "/" + prefix;
        return normalizedPrefix.endsWith("/")
                ? normalizedPrefix + objectKey
                : normalizedPrefix + "/" + objectKey;
    }

    @Override
    public String generateSignedUrl(String objectKey, Duration expiration) {
        return getPublicUrl(objectKey);
    }
}
```

- [ ] **Step 4: Implement Aliyun provider**

Create `backend/src/main/java/com/mianshiba/ai/service/storage/impl/AliyunOssFileStorageService.java`:

```java
package com.mianshiba.ai.service.storage.impl;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.mianshiba.ai.config.FileUploadProperties;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.service.storage.FileStorageService;
import com.mianshiba.ai.service.storage.StoredFile;
import com.mianshiba.ai.service.storage.UploadFileCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.file", name = "provider", havingValue = "aliyun")
public class AliyunOssFileStorageService implements FileStorageService {

    private final FileUploadProperties fileUploadProperties;

    @Override
    public StoredFile upload(UploadFileCommand command) {
        FileUploadProperties.Aliyun aliyun = requireAliyunConfig();
        OSS ossClient = new OSSClientBuilder().build(
                aliyun.getEndpoint(), aliyun.getAccessKeyId(), aliyun.getAccessKeySecret());
        try {
            ossClient.putObject(aliyun.getBucket(), command.objectKey(), command.inputStream());
            return new StoredFile(command.objectKey(), getPublicUrl(command.objectKey()),
                    command.originalName(), command.contentType(), command.size());
        } catch (Exception ex) {
            log.error("aliyun_oss_upload_failed bucket={} objectKey={}", aliyun.getBucket(), command.objectKey(), ex);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public String getPublicUrl(String objectKey) {
        FileUploadProperties.Aliyun aliyun = requireAliyunConfig();
        if (StringUtils.hasText(aliyun.getDomain())) {
            return trimTrailingSlash(aliyun.getDomain()) + "/" + objectKey;
        }
        String endpoint = aliyun.getEndpoint().replace("https://", "").replace("http://", "");
        return "https://" + aliyun.getBucket() + "." + endpoint + "/" + objectKey;
    }

    @Override
    public String generateSignedUrl(String objectKey, Duration expiration) {
        FileUploadProperties.Aliyun aliyun = requireAliyunConfig();
        OSS ossClient = new OSSClientBuilder().build(
                aliyun.getEndpoint(), aliyun.getAccessKeyId(), aliyun.getAccessKeySecret());
        try {
            Date expiresAt = new Date(System.currentTimeMillis() + expiration.toMillis());
            return ossClient.generatePresignedUrl(aliyun.getBucket(), objectKey, expiresAt, HttpMethod.GET).toString();
        } catch (Exception ex) {
            log.error("aliyun_oss_signed_url_failed bucket={} objectKey={}", aliyun.getBucket(), objectKey, ex);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "录音播放地址生成失败");
        } finally {
            ossClient.shutdown();
        }
    }

    private FileUploadProperties.Aliyun requireAliyunConfig() {
        FileUploadProperties.Aliyun aliyun = fileUploadProperties.getAliyun();
        if (!StringUtils.hasText(aliyun.getEndpoint())
                || !StringUtils.hasText(aliyun.getBucket())
                || !StringUtils.hasText(aliyun.getAccessKeyId())
                || !StringUtils.hasText(aliyun.getAccessKeySecret())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "OSS 配置不完整");
        }
        return aliyun;
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
```

- [ ] **Step 5: Run local storage tests**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=LocalFileStorageServiceTest
```

Expected: PASS.

## Task 3: Business File Service For Avatar And Audio

**Files:**

- Modify: `backend/src/main/java/com/mianshiba/ai/service/FileService.java`
- Modify or rename: `backend/src/main/java/com/mianshiba/ai/service/impl/LocalFileServiceImpl.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/model/vo/FileUploadVO.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/FileServiceImplTest.java`

- [ ] **Step 1: Write file service tests**

Create `backend/src/test/java/com/mianshiba/ai/service/impl/FileServiceImplTest.java`:

```java
package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.config.FileUploadProperties;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.model.vo.FileUploadVO;
import com.mianshiba.ai.service.storage.FileStorageService;
import com.mianshiba.ai.service.storage.StoredFile;
import com.mianshiba.ai.service.storage.UploadFileCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileServiceImplTest {

    private FileStorageService storageService;
    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        storageService = mock(FileStorageService.class);
        FileUploadProperties properties = new FileUploadProperties();
        properties.setSignedUrlExpiration(Duration.ofMinutes(10));
        properties.getAvatar().setMaxSize(2 * 1024 * 1024L);
        properties.getAudio().setMaxSize(20 * 1024 * 1024L);
        fileService = new FileServiceImpl(properties, storageService);
    }

    @Test
    void uploadAvatarRejectsInvalidType() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.gif", "image/gif", new byte[]{1});

        assertThatThrownBy(() -> fileService.uploadAvatar(file))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
    }

    @Test
    void uploadAvatarStoresImageUnderAvatarDirectory() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2});
        when(storageService.upload(any(UploadFileCommand.class))).thenReturn(
                new StoredFile("avatar/100.png", "/uploads/avatar/100.png", "avatar.png", "image/png", 2));

        FileUploadVO result = fileService.uploadAvatar(file);

        ArgumentCaptor<UploadFileCommand> captor = ArgumentCaptor.forClass(UploadFileCommand.class);
        verify(storageService).upload(captor.capture());
        assertThat(captor.getValue().objectKey()).startsWith("avatar/").endsWith(".png");
        assertThat(result.getUrl()).isEqualTo("/uploads/avatar/100.png");
        assertThat(result.getObjectKey()).isEqualTo("avatar/100.png");
        assertThat(result.getContentType()).isEqualTo("image/png");
    }

    @Test
    void uploadInterviewAudioStoresAudioUnderInterviewDirectory() {
        MockMultipartFile file = new MockMultipartFile("file", "answer.webm", "audio/webm", new byte[]{1, 2, 3});
        when(storageService.upload(any(UploadFileCommand.class))).thenReturn(
                new StoredFile("interview/100/200/audio.webm", "/uploads/interview/100/200/audio.webm", "answer.webm", "audio/webm", 3));

        FileUploadVO result = fileService.uploadInterviewAudio(100L, 200L, file);

        ArgumentCaptor<UploadFileCommand> captor = ArgumentCaptor.forClass(UploadFileCommand.class);
        verify(storageService).upload(captor.capture());
        assertThat(captor.getValue().objectKey()).startsWith("interview/100/200/").endsWith(".webm");
        assertThat(result.getObjectKey()).isEqualTo("interview/100/200/audio.webm");
        assertThat(result.getContentType()).isEqualTo("audio/webm");
    }

    @Test
    void generateSignedUrlDelegatesToStorageWithConfiguredExpiration() {
        when(storageService.generateSignedUrl(eq("interview/100/200/audio.webm"), eq(Duration.ofMinutes(10))))
                .thenReturn("https://signed.example.com/audio.webm");

        String url = fileService.generateSignedUrl("interview/100/200/audio.webm");

        assertThat(url).isEqualTo("https://signed.example.com/audio.webm");
    }
}
```

- [ ] **Step 2: Run tests to verify failure**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=FileServiceImplTest
```

Expected: FAIL because `FileServiceImpl`, `uploadInterviewAudio`, `generateSignedUrl`, and extra VO fields do not exist.

- [ ] **Step 3: Update FileUploadVO**

Update `backend/src/main/java/com/mianshiba/ai/model/vo/FileUploadVO.java` by adding fields while keeping existing ones:

```java
@Schema(description = "文件对象 Key")
private String objectKey;

@Schema(description = "文件 Content-Type")
private String contentType;
```

- [ ] **Step 4: Update FileService interface**

Update `backend/src/main/java/com/mianshiba/ai/service/FileService.java`:

```java
package com.mianshiba.ai.service;

import com.mianshiba.ai.model.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    FileUploadVO uploadAvatar(MultipartFile file);

    FileUploadVO uploadInterviewAudio(Long sessionId, Long turnId, MultipartFile file);

    String generateSignedUrl(String objectKey);
}
```

- [ ] **Step 5: Replace old local-specific business implementation**

If `backend/src/main/java/com/mianshiba/ai/service/impl/LocalFileServiceImpl.java` exists, rename the class and file to `FileServiceImpl.java`. The final implementation should be:

```java
package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.config.FileUploadProperties;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.model.vo.FileUploadVO;
import com.mianshiba.ai.service.FileService;
import com.mianshiba.ai.service.storage.StoredFile;
import com.mianshiba.ai.service.storage.UploadFileCommand;
import com.mianshiba.ai.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final Set<String> ALLOWED_AVATAR_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_AVATAR_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_AUDIO_CONTENT_TYPES = Set.of("audio/webm", "audio/mp4", "audio/mpeg", "audio/wav", "audio/x-wav");
    private static final Set<String> ALLOWED_AUDIO_EXTENSIONS = Set.of("webm", "mp4", "mp3", "wav");

    private final FileUploadProperties fileUploadProperties;
    private final FileStorageService fileStorageService;

    @Override
    public FileUploadVO uploadAvatar(MultipartFile file) {
        validateFile(file, fileUploadProperties.getAvatar().getMaxSize(), ALLOWED_AVATAR_CONTENT_TYPES,
                ALLOWED_AVATAR_EXTENSIONS, "请选择头像文件", "头像不能超过 2MB", "仅支持 JPG、PNG、WebP 图片");
        String extension = getExtension(file.getOriginalFilename());
        return upload(file, "avatar/" + UUID.randomUUID() + "." + extension);
    }

    @Override
    public FileUploadVO uploadInterviewAudio(Long sessionId, Long turnId, MultipartFile file) {
        validateFile(file, fileUploadProperties.getAudio().getMaxSize(), ALLOWED_AUDIO_CONTENT_TYPES,
                ALLOWED_AUDIO_EXTENSIONS, "请选择录音文件", "录音文件不能超过 20MB", "仅支持 WebM、MP4、MP3、WAV 音频");
        String extension = getExtension(file.getOriginalFilename());
        return upload(file, "interview/" + sessionId + "/" + turnId + "/" + UUID.randomUUID() + "." + extension);
    }

    @Override
    public String generateSignedUrl(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件对象 Key 不能为空");
        }
        return fileStorageService.generateSignedUrl(objectKey, fileUploadProperties.getSignedUrlExpiration());
    }

    private FileUploadVO upload(MultipartFile file, String objectKey) {
        try {
            StoredFile storedFile = fileStorageService.upload(new UploadFileCommand(
                    file.getInputStream(), objectKey, file.getOriginalFilename(), file.getContentType(), file.getSize()));
            return FileUploadVO.builder()
                    .url(storedFile.url())
                    .objectKey(storedFile.objectKey())
                    .originalName(storedFile.originalName())
                    .contentType(storedFile.contentType())
                    .size(storedFile.size())
                    .build();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }
    }

    private void validateFile(MultipartFile file, long maxSize, Set<String> allowedContentTypes,
                              Set<String> allowedExtensions, String emptyMessage, String sizeMessage, String typeMessage) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, emptyMessage);
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, sizeMessage);
        }
        String contentType = file.getContentType();
        if (!allowedContentTypes.contains(contentType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, typeMessage);
        }
        String extension = getExtension(file.getOriginalFilename());
        if (!allowedExtensions.contains(extension)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件后缀不合法");
        }
    }

    private String getExtension(String originalFilename) {
        String filename = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件后缀不能为空");
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
```

- [ ] **Step 6: Run file service tests**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=FileServiceImplTest
```

Expected: PASS.

## Task 4: Database, Entity, And VO Audio Metadata

**Files:**

- Modify: `backend/src/main/resources/sql/init.sql`
- Modify: `backend/src/main/java/com/mianshiba/ai/model/entity/InterviewTurn.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewTurnVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewAudioUploadVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewAudioUrlVO.java`

- [ ] **Step 1: Add SQL column test**

Modify an existing SQL test under `backend/src/test/java/com/mianshiba/ai/sql/`, or create `backend/src/test/java/com/mianshiba/ai/sql/InitSqlInterviewAudioTest.java`:

```java
package com.mianshiba.ai.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InitSqlInterviewAudioTest {

    @Test
    void interviewTurnContainsAudioMetadataColumns() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/sql/init.sql"));

        assertThat(sql).contains("answer_audio_object_key VARCHAR(512)");
        assertThat(sql).contains("answer_audio_original_name VARCHAR(255)");
        assertThat(sql).contains("answer_audio_size BIGINT");
        assertThat(sql).contains("answer_audio_content_type VARCHAR(128)");
        assertThat(sql).contains("answer_audio_duration_seconds INT");
    }
}
```

- [ ] **Step 2: Run SQL test to verify failure**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=InitSqlInterviewAudioTest
```

Expected: FAIL because columns are not present.

- [ ] **Step 3: Add columns to init.sql**

In `backend/src/main/resources/sql/init.sql`, update `CREATE TABLE IF NOT EXISTS interview_turn` after `answer_duration_seconds`:

```sql
  answer_duration_seconds INT DEFAULT NULL COMMENT '回答耗时秒数',
  answer_audio_object_key VARCHAR(512) NOT NULL DEFAULT '' COMMENT '回答录音对象 Key',
  answer_audio_original_name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '回答录音原始文件名',
  answer_audio_size BIGINT NOT NULL DEFAULT 0 COMMENT '回答录音文件大小',
  answer_audio_content_type VARCHAR(128) NOT NULL DEFAULT '' COMMENT '回答录音 Content-Type',
  answer_audio_duration_seconds INT NOT NULL DEFAULT 0 COMMENT '回答录音时长秒数',
```

- [ ] **Step 4: Extend InterviewTurn entity**

Add fields to `backend/src/main/java/com/mianshiba/ai/model/entity/InterviewTurn.java` after `answerDurationSeconds`:

```java
private String answerAudioObjectKey;

private String answerAudioOriginalName;

private Long answerAudioSize;

private String answerAudioContentType;

private Integer answerAudioDurationSeconds;
```

- [ ] **Step 5: Extend InterviewTurnVO**

Add fields to `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewTurnVO.java`:

```java
private Boolean hasAnswerAudio;

private String answerAudioOriginalName;

private Long answerAudioSize;

private String answerAudioContentType;

private Integer answerAudioDurationSeconds;
```

- [ ] **Step 6: Add audio response VOs**

Create `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewAudioUploadVO.java`:

```java
package com.mianshiba.ai.model.vo.interview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "面试回答录音上传结果")
public class InterviewAudioUploadVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long turnId;

    private Boolean hasAnswerAudio;
    private String originalName;
    private Long size;
    private String contentType;
    private Integer durationSeconds;
}
```

Create `backend/src/main/java/com/mianshiba/ai/model/vo/interview/InterviewAudioUrlVO.java`:

```java
package com.mianshiba.ai.model.vo.interview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "面试回答录音播放地址")
public class InterviewAudioUrlVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String url;
    private Instant expiresAt;
}
```

- [ ] **Step 7: Run SQL test**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=InitSqlInterviewAudioTest
```

Expected: PASS.

## Task 5: Interview Audio Backend Endpoints

**Files:**

- Modify: `backend/src/main/java/com/mianshiba/ai/service/InterviewService.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/InterviewServiceImpl.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/controller/InterviewController.java`
- Modify: `backend/src/test/java/com/mianshiba/ai/service/impl/InterviewServiceImplTest.java`

- [ ] **Step 1: Add service tests for ownership and metadata persistence**

Modify `InterviewServiceImplTest` constructor setup to include a mocked `FileService` dependency after adding it to `InterviewServiceImpl`. Add tests:

```java
@Mock
private FileService fileService;
```

Update construction:

```java
interviewService = new InterviewServiceImpl(
        interviewSessionMapper, interviewTurnMapper, interviewReportMapper,
        resumeMapper, userMapper, jwtUtils, chatClient, speechService,
        jobMapper, jobAnalysisMapper, interviewReportEnhancementService, fileService);
```

Add test methods:

```java
@Test
void uploadAnswerAudioStoresMetadataForOwnedTurn() {
    when(userMapper.selectById(1001L)).thenReturn(normalUser());
    when(interviewSessionMapper.selectById(100L)).thenReturn(inProgressSession());
    InterviewTurn turn = unansweredTurn();
    when(interviewTurnMapper.selectById(200L)).thenReturn(turn);
    when(interviewTurnMapper.updateById(any(InterviewTurn.class))).thenReturn(1);

    MockMultipartFile file = new MockMultipartFile("file", "answer.webm", "audio/webm", new byte[]{1, 2, 3});
    FileUploadVO upload = FileUploadVO.builder()
            .objectKey("interview/100/200/audio.webm")
            .originalName("answer.webm")
            .contentType("audio/webm")
            .size(3L)
            .build();
    when(fileService.uploadInterviewAudio(100L, 200L, file)).thenReturn(upload);

    String auth = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");
    InterviewAudioUploadVO result = interviewService.uploadAnswerAudio(auth, 100L, 200L, file, 12);

    assertThat(result.getTurnId()).isEqualTo(200L);
    assertThat(result.getHasAnswerAudio()).isTrue();
    assertThat(turn.getAnswerAudioObjectKey()).isEqualTo("interview/100/200/audio.webm");
    assertThat(turn.getAnswerAudioDurationSeconds()).isEqualTo(12);
    verify(interviewTurnMapper).updateById(turn);
}

@Test
void getAnswerAudioUrlRejectsTurnWithoutAudio() {
    when(userMapper.selectById(1001L)).thenReturn(normalUser());
    when(interviewSessionMapper.selectById(100L)).thenReturn(inProgressSession());
    InterviewTurn turn = unansweredTurn();
    turn.setAnswerAudioObjectKey("");
    when(interviewTurnMapper.selectById(200L)).thenReturn(turn);

    String auth = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");

    assertThatThrownBy(() -> interviewService.getAnswerAudioUrl(auth, 100L, 200L))
            .isInstanceOf(BusinessException.class)
            .extracting("code")
            .isEqualTo(ErrorCode.NOT_FOUND_ERROR.getCode());
}

@Test
void getAnswerAudioUrlReturnsSignedUrlForOwnedTurn() {
    when(userMapper.selectById(1001L)).thenReturn(normalUser());
    when(interviewSessionMapper.selectById(100L)).thenReturn(inProgressSession());
    InterviewTurn turn = unansweredTurn();
    turn.setAnswerAudioObjectKey("interview/100/200/audio.webm");
    when(interviewTurnMapper.selectById(200L)).thenReturn(turn);
    when(fileService.generateSignedUrl("interview/100/200/audio.webm"))
            .thenReturn("https://signed.example.com/audio.webm");

    String auth = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");
    InterviewAudioUrlVO result = interviewService.getAnswerAudioUrl(auth, 100L, 200L);

    assertThat(result.getUrl()).isEqualTo("https://signed.example.com/audio.webm");
    assertThat(result.getExpiresAt()).isNotNull();
}
```

Required imports:

```java
import com.mianshiba.ai.model.vo.FileUploadVO;
import com.mianshiba.ai.model.vo.interview.InterviewAudioUploadVO;
import com.mianshiba.ai.model.vo.interview.InterviewAudioUrlVO;
import com.mianshiba.ai.service.FileService;
import org.springframework.mock.web.MockMultipartFile;
```

- [ ] **Step 2: Run interview tests to verify failure**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=InterviewServiceImplTest
```

Expected: FAIL because service methods and constructor dependency do not exist.

- [ ] **Step 3: Extend InterviewService interface**

Add to `backend/src/main/java/com/mianshiba/ai/service/InterviewService.java`:

```java
InterviewAudioUploadVO uploadAnswerAudio(String authorizationHeader, Long sessionId, Long turnId,
                                         MultipartFile file, Integer durationSeconds);

InterviewAudioUrlVO getAnswerAudioUrl(String authorizationHeader, Long sessionId, Long turnId);
```

Add imports:

```java
import com.mianshiba.ai.model.vo.interview.InterviewAudioUploadVO;
import com.mianshiba.ai.model.vo.interview.InterviewAudioUrlVO;
import org.springframework.web.multipart.MultipartFile;
```

- [ ] **Step 4: Implement service methods**

Modify `InterviewServiceImpl` constructor dependencies by adding:

```java
private final FileService fileService;
```

Add imports:

```java
import com.mianshiba.ai.model.vo.FileUploadVO;
import com.mianshiba.ai.model.vo.interview.InterviewAudioUploadVO;
import com.mianshiba.ai.model.vo.interview.InterviewAudioUrlVO;
import com.mianshiba.ai.service.FileService;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.time.Instant;
```

Add methods before `getSession`:

```java
@Override
@Transactional(rollbackFor = Exception.class)
public InterviewAudioUploadVO uploadAnswerAudio(String authorizationHeader, Long sessionId, Long turnId,
                                                MultipartFile file, Integer durationSeconds) {
    Long userId = resolveUserId(authorizationHeader);
    getSessionAndCheckOwner(sessionId, userId);
    InterviewTurn turn = getTurnAndCheckSession(sessionId, turnId);

    FileUploadVO upload = fileService.uploadInterviewAudio(sessionId, turnId, file);
    turn.setAnswerAudioObjectKey(upload.getObjectKey());
    turn.setAnswerAudioOriginalName(upload.getOriginalName());
    turn.setAnswerAudioSize(upload.getSize());
    turn.setAnswerAudioContentType(upload.getContentType());
    turn.setAnswerAudioDurationSeconds(durationSeconds != null ? Math.max(durationSeconds, 0) : 0);
    interviewTurnMapper.updateById(turn);

    return InterviewAudioUploadVO.builder()
            .turnId(turn.getId())
            .hasAnswerAudio(true)
            .originalName(turn.getAnswerAudioOriginalName())
            .size(turn.getAnswerAudioSize())
            .contentType(turn.getAnswerAudioContentType())
            .durationSeconds(turn.getAnswerAudioDurationSeconds())
            .build();
}

@Override
public InterviewAudioUrlVO getAnswerAudioUrl(String authorizationHeader, Long sessionId, Long turnId) {
    Long userId = resolveUserId(authorizationHeader);
    getSessionAndCheckOwner(sessionId, userId);
    InterviewTurn turn = getTurnAndCheckSession(sessionId, turnId);
    if (!StringUtils.hasText(turn.getAnswerAudioObjectKey())) {
        throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "本轮暂无录音");
    }
    return InterviewAudioUrlVO.builder()
            .url(fileService.generateSignedUrl(turn.getAnswerAudioObjectKey()))
            .expiresAt(Instant.now().plusSeconds(600))
            .build();
}

private InterviewTurn getTurnAndCheckSession(Long sessionId, Long turnId) {
    InterviewTurn turn = interviewTurnMapper.selectById(turnId);
    if (turn == null || !turn.getSessionId().equals(sessionId)) {
        throw new BusinessException(ErrorCode.INTERVIEW_TURN_ERROR);
    }
    return turn;
}
```

Update `submitAnswer` to use `getTurnAndCheckSession(sessionId, turnId)` instead of duplicating the turn lookup.

Update `toInterviewTurnVO`:

```java
vo.setHasAnswerAudio(StringUtils.hasText(turn.getAnswerAudioObjectKey()));
vo.setAnswerAudioOriginalName(turn.getAnswerAudioOriginalName());
vo.setAnswerAudioSize(turn.getAnswerAudioSize());
vo.setAnswerAudioContentType(turn.getAnswerAudioContentType());
vo.setAnswerAudioDurationSeconds(turn.getAnswerAudioDurationSeconds());
```

- [ ] **Step 5: Add controller endpoints**

Modify `InterviewController` imports:

```java
import com.mianshiba.ai.model.vo.interview.InterviewAudioUploadVO;
import com.mianshiba.ai.model.vo.interview.InterviewAudioUrlVO;
import org.springframework.web.multipart.MultipartFile;
```

Add endpoints after `submitAnswer`:

```java
@PostMapping("/session/{sessionId}/turn/{turnId}/audio")
@Operation(summary = "上传面试回答录音")
public BaseResponse<InterviewAudioUploadVO> uploadAnswerAudio(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
        @PathVariable("sessionId") Long sessionId,
        @PathVariable("turnId") Long turnId,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "durationSeconds", required = false) Integer durationSeconds) {
    return ResultUtils.success(interviewService.uploadAnswerAudio(
            authorizationHeader, sessionId, turnId, file, durationSeconds));
}

@GetMapping("/session/{sessionId}/turn/{turnId}/audio-url")
@Operation(summary = "获取面试回答录音播放地址")
public BaseResponse<InterviewAudioUrlVO> getAnswerAudioUrl(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
        @PathVariable("sessionId") Long sessionId,
        @PathVariable("turnId") Long turnId) {
    return ResultUtils.success(interviewService.getAnswerAudioUrl(authorizationHeader, sessionId, turnId));
}
```

- [ ] **Step 6: Run interview service tests**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=InterviewServiceImplTest
```

Expected: PASS.

## Task 6: Frontend API, Types, And Store Wiring

**Files:**

- Modify: `frontend/src/types/interview.ts`
- Modify: `frontend/src/api/interview.ts`
- Modify: `frontend/src/stores/interview.ts`

- [ ] **Step 1: Extend frontend types**

Modify `frontend/src/types/interview.ts`:

```ts
export interface InterviewTurnVO {
  id: number
  sessionId: number
  questionNo: number
  turnType: InterviewTurnType
  questionText: string
  answerText: string | null
  aiFeedback: string | null
  answerDurationSeconds: number | null
  hasAnswerAudio: boolean
  answerAudioOriginalName: string | null
  answerAudioSize: number | null
  answerAudioContentType: string | null
  answerAudioDurationSeconds: number | null
  createTime: string
  updateTime: string
}

export interface InterviewAudioUploadVO {
  turnId: number
  hasAnswerAudio: boolean
  originalName: string | null
  size: number | null
  contentType: string | null
  durationSeconds: number | null
}

export interface InterviewAudioUrlVO {
  url: string
  expiresAt: string
}
```

- [ ] **Step 2: Add API functions**

Modify `frontend/src/api/interview.ts` imports to include the new types and add:

```ts
export function uploadInterviewAnswerAudio(
  sessionId: number,
  turnId: number,
  file: File | Blob,
  durationSeconds: number,
) {
  const formData = new FormData()
  formData.append('file', file, 'answer.webm')
  formData.append('durationSeconds', String(durationSeconds))
  return request.post<BaseResponse<InterviewAudioUploadVO>>(
    `/api/interview/session/${sessionId}/turn/${turnId}/audio`,
    formData,
  )
}

export function getInterviewAnswerAudioUrl(sessionId: number, turnId: number) {
  return request.get<BaseResponse<InterviewAudioUrlVO>>(
    `/api/interview/session/${sessionId}/turn/${turnId}/audio-url`,
  )
}
```

- [ ] **Step 3: Add store actions if store wraps all interview API calls**

Inspect `frontend/src/stores/interview.ts`. If it wraps existing API functions, add:

```ts
async function uploadAnswerAudio(sessionId: number, turnId: number, file: File | Blob, durationSeconds: number) {
  const res = await uploadInterviewAnswerAudio(sessionId, turnId, file, durationSeconds)
  return res.data
}

async function fetchAnswerAudioUrl(sessionId: number, turnId: number) {
  const res = await getInterviewAnswerAudioUrl(sessionId, turnId)
  return res.data.url
}
```

Ensure these functions are returned from the Pinia setup store.

- [ ] **Step 4: Run frontend type check**

Run from `frontend/`:

```powershell
npm run type-check
```

Expected: PASS or only failures caused by later tasks not yet implemented if types are referenced before components are edited.

## Task 7: Interview Room Opt-In Recording Upload

**Files:**

- Modify: `frontend/src/views/interview/InterviewRoomPage.vue`

- [ ] **Step 1: Add state for opt-in audio persistence**

In `<script setup>`, add refs and recorder variables near existing recording state:

```ts
const saveAnswerAudio = ref(false)
const isUploadingAnswerAudio = ref(false)
let mediaRecorder: MediaRecorder | null = null
let recordedChunks: BlobPart[] = []
let pendingAnswerAudioBlob: Blob | null = null
let pendingAnswerAudioTurnId: number | null = null
let pendingAnswerAudioDurationSeconds = 0
```

- [ ] **Step 2: Add UI switch**

Near the recording controls in the template, add:

```vue
<el-switch
  v-model="saveAnswerAudio"
  active-text="保存本轮录音"
  inactive-text="不保存录音"
  :disabled="state === 'recording' || state === 'submittingAnswer'"
/>
<p class="interview-room__audio-tip">
  录音保存默认关闭，开启后本轮回答音频会保存到云端用于复盘。
</p>
```

- [ ] **Step 3: Start MediaRecorder alongside existing ASR audio flow**

After `mediaStream = await navigator.mediaDevices.getUserMedia(...)` in `startRecording`, add:

```ts
if (saveAnswerAudio.value && typeof MediaRecorder !== 'undefined') {
  recordedChunks = []
  const mimeType = MediaRecorder.isTypeSupported('audio/webm') ? 'audio/webm' : ''
  mediaRecorder = new MediaRecorder(mediaStream, mimeType ? { mimeType } : undefined)
  mediaRecorder.ondataavailable = (event) => {
    if (event.data.size > 0) {
      recordedChunks.push(event.data)
    }
  }
  mediaRecorder.onstop = () => {
    pendingAnswerAudioBlob = new Blob(recordedChunks, { type: mediaRecorder?.mimeType || 'audio/webm' })
    pendingAnswerAudioTurnId = currentQuestion.value?.turnId || null
    pendingAnswerAudioDurationSeconds = Math.round((Date.now() - recordingStartTime) / 1000)
  }
  mediaRecorder.start()
}
```

- [ ] **Step 4: Stop MediaRecorder before cleanup**

In `stopRecording`, before `cleanupAudio()`, add:

```ts
if (mediaRecorder && mediaRecorder.state !== 'inactive') {
  mediaRecorder.stop()
}
```

In `cleanupAudio`, also set:

```ts
mediaRecorder = null
```

- [ ] **Step 5: Upload audio after answer submission succeeds**

Import the API or use the store action from Task 6. Add helper:

```ts
async function uploadPendingAnswerAudio() {
  if (!pendingAnswerAudioBlob || !pendingAnswerAudioTurnId) return
  isUploadingAnswerAudio.value = true
  try {
    await interviewStore.uploadAnswerAudio(
      sessionId.value,
      pendingAnswerAudioTurnId,
      pendingAnswerAudioBlob,
      pendingAnswerAudioDurationSeconds,
    )
    ElMessage.success('录音已保存')
  } catch {
    ElMessage.warning('录音保存失败，可稍后重试')
  } finally {
    isUploadingAnswerAudio.value = false
    pendingAnswerAudioBlob = null
    pendingAnswerAudioTurnId = null
    pendingAnswerAudioDurationSeconds = 0
    recordedChunks = []
  }
}
```

In `submitAnswer`, after `if (res.code === 0 && res.data) {`, before changing `currentQuestion` to the next turn, call:

```ts
await uploadPendingAnswerAudio()
```

This ensures the audio uploads against the answered turn before the next question replaces `currentQuestion`.

- [ ] **Step 6: Add minimal styles**

Add scoped CSS:

```css
.interview-room__audio-tip {
  margin: 8px 0 0;
  color: var(--nb-text-muted);
  font-size: 13px;
}
```

- [ ] **Step 7: Run type check**

Run from `frontend/`:

```powershell
npm run type-check
```

Expected: PASS.

## Task 8: Interview Report Playback UI

**Files:**

- Modify: `frontend/src/views/interview/InterviewReportPage.vue`

- [ ] **Step 1: Add playback state and helper**

Update imports:

```ts
import { computed, onMounted, onUnmounted, reactive } from 'vue'
```

Add state:

```ts
const audioUrls = reactive<Record<number, string>>({})
const loadingAudioTurnIds = reactive<Record<number, boolean>>({})
```

Add helper:

```ts
async function loadTurnAudioUrl(turnId: number) {
  if (audioUrls[turnId] || loadingAudioTurnIds[turnId]) return
  loadingAudioTurnIds[turnId] = true
  try {
    audioUrls[turnId] = await interviewStore.fetchAnswerAudioUrl(sessionId.value, turnId)
  } catch {
    ElMessage.warning('录音暂不可播放')
  } finally {
    loadingAudioTurnIds[turnId] = false
  }
}
```

- [ ] **Step 2: Add playback block inside each turn**

In the turn detail block after answer text, add:

```vue
<div v-if="turn.hasAnswerAudio" class="interview-report-page__turn-block">
  <div class="interview-report-page__turn-label">回答录音</div>
  <NbButton
    v-if="!audioUrls[turn.id]"
    variant="ghost"
    :loading="loadingAudioTurnIds[turn.id]"
    @click="loadTurnAudioUrl(turn.id)"
  >
    加载录音
  </NbButton>
  <audio v-else controls :src="audioUrls[turn.id]" class="interview-report-page__audio"></audio>
  <div class="interview-report-page__audio-meta">
    {{ turn.answerAudioDurationSeconds || 0 }} 秒 · {{ turn.answerAudioContentType || 'audio' }}
  </div>
</div>
```

- [ ] **Step 3: Add styles**

Add scoped CSS:

```css
.interview-report-page__audio {
  width: 100%;
  max-width: 520px;
  margin-top: 8px;
}

.interview-report-page__audio-meta {
  margin-top: 6px;
  color: var(--nb-text-muted);
  font-size: 13px;
}
```

- [ ] **Step 4: Run type check**

Run from `frontend/`:

```powershell
npm run type-check
```

Expected: PASS.

## Task 9: Full Verification

**Files:**

- All files changed by previous tasks.

- [ ] **Step 1: Run focused backend tests**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=FileUploadPropertiesTest,LocalFileStorageServiceTest,FileServiceImplTest,InterviewServiceImplTest,InitSqlInterviewAudioTest
```

Expected: PASS.

- [ ] **Step 2: Run full backend tests**

Run from `backend/`:

```powershell
.\mvnw.cmd test
```

Expected: PASS. If a failure is unrelated to OSS changes, capture the failing test name and error before deciding whether to fix or report it.

- [ ] **Step 3: Run frontend type check**

Run from `frontend/`:

```powershell
npm run type-check
```

Expected: PASS.

- [ ] **Step 4: Run frontend build**

Run from `frontend/`:

```powershell
npm run build
```

Expected: PASS.

- [ ] **Step 5: Inspect diff**

Run from repository root:

```powershell
git diff -- backend/src/main/java backend/src/main/resources backend/src/test/java frontend/src docs/superpowers
```

Expected: Diff only contains OSS storage, avatar refactor, interview audio persistence/playback, and documentation/plan changes. Do not revert unrelated pre-existing worktree changes.
