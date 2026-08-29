package com.ecommerce.backend.product;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductImageStorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_GIF_VALUE,
            "image/webp"
    );

    private final Path storageDirectory;

    public ProductImageStorageService(@Value("${app.uploads.directory:uploads/products}") String directory) {
        this.storageDirectory = Paths.get(directory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        validate(file);

        try {
            Files.createDirectories(storageDirectory);
            String extension = extension(file.getOriginalFilename(), file.getContentType());
            String filename = UUID.randomUUID() + extension;
            Path target = storageDirectory.resolve(filename).normalize();

            if (!target.getParent().equals(storageDirectory)) {
                throw new IllegalArgumentException("Invalid image filename");
            }

            file.transferTo(target);
            return "/uploads/products/" + filename;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store product image", exception);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Image file must not exceed 5 MB");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Only JPEG, PNG, GIF and WebP images are allowed");
        }
    }

    private String extension(String originalFilename, String contentType) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension != null && !extension.isBlank()) {
            return "." + extension.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        }
        return switch (contentType) {
            case MediaType.IMAGE_PNG_VALUE -> ".png";
            case MediaType.IMAGE_GIF_VALUE -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
