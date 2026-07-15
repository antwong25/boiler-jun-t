package org.example.boilerserver.controller;

import org.example.boilercommon.Result;
import org.example.boilerpojo.PostImageUploadDTO;
import org.example.boilerserver.config.FileStorageProperties;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/file")
public class FileUploadController {
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".gif", ".webp");

    private final FileStorageProperties fileStorageProperties;

    public FileUploadController(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    @PostMapping("/upload/post-image")
    public Result<PostImageUploadDTO> uploadPostImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalFileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "post-image";
        String safeExtension = extractSafeExtension(originalFileName);
        validateImageExtension(safeExtension);
        
        String storedFileName = "post-" + UUID.randomUUID().toString().replace("-", "") + safeExtension;
        Path targetDirectory = resolvePostImagesDirectory();
        Path targetFile = targetDirectory.resolve(storedFileName).normalize();

        try {
            Files.createDirectories(targetDirectory);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("保存图片失败");
        }

        String relativeUrl = "/uploads/post-images/" + storedFileName;

        PostImageUploadDTO result = new PostImageUploadDTO();
        result.setFileName(storedFileName);
        result.setFileUrl(relativeUrl);
        return Result.success(result);
    }

    private Path resolvePostImagesDirectory() {
        return Paths.get(fileStorageProperties.getPostImagesDir())
                .toAbsolutePath()
                .normalize();
    }

    private String extractSafeExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        String extension = fileName.substring(index).toLowerCase(Locale.ROOT);
        return extension.matches("\\.[a-z0-9]{1,10}") ? extension : "";
    }

    private void validateImageExtension(String extension) {
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 png、jpg、jpeg、gif 或 webp 格式的图片");
        }
    }
}
