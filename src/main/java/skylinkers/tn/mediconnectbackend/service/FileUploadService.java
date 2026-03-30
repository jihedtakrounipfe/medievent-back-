package skylinkers.tn.mediconnectbackend.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import skylinkers.tn.mediconnectbackend.exception.FileUploadException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileUploadService {

    public record UploadResponse(String url) {}

    private static final long MAX_BYTES = 5L * 1024L * 1024L;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/webp"
    );

    @Value("${mediconnect.upload.image-path:C:\\\\Users\\\\amine\\\\Desktop\\\\MediConnect\\\\Images}")
    private String imagePath;

    @PostConstruct
    public void init() {
        try {
            Path dir = Paths.get(imagePath);
            Files.createDirectories(dir);
            log.info("Upload directory ready: {}", dir.toAbsolutePath());
        } catch (IOException e) {
            throw new FileUploadException("Unable to initialize upload directory.", e);
        }
    }

    /**
     * Uploads an image and returns its public URL.
     */
    public UploadResponse uploadImage(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File is required.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new FileUploadException("File size exceeds 5MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new FileUploadException("Invalid file type. Only PNG, JPG, JPEG, GIF, WEBP are allowed.");
        }

        if (!looksLikeImage(file, contentType)) {
            throw new FileUploadException("Invalid image content.");
        }

        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
        String safeOriginal = original.replaceAll("[^a-zA-Z0-9._-]", "_");

        String prefix = (userId == null ? "anon" : userId.toString());
        String filename = prefix + "_" + Instant.now().toEpochMilli() + "_" + UUID.randomUUID() + "_" + safeOriginal;

        Path target = Paths.get(imagePath).resolve(filename).normalize();
        if (!target.startsWith(Paths.get(imagePath).normalize())) {
            throw new FileUploadException("Invalid file path.");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileUploadException("Failed to store file.", e);
        }

        return new UploadResponse("/api/files/images/" + filename);
    }

    public Path resolveImage(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new FileUploadException("Filename is required.");
        }
        String clean = filename.replace("\\", "/");
        if (clean.contains("..") || clean.contains("/")) {
            throw new FileUploadException("Invalid filename.");
        }
        Path p = Paths.get(imagePath).resolve(clean).normalize();
        if (!p.startsWith(Paths.get(imagePath).normalize())) {
            throw new FileUploadException("Invalid file path.");
        }
        return p;
    }

    private boolean looksLikeImage(MultipartFile file, String contentType) {
        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(16);
            if (header.length < 4) return false;
            return switch (contentType) {
                case "image/png" -> isPng(header);
                case "image/jpeg" -> isJpeg(header);
                case "image/gif" -> isGif(header);
                case "image/webp" -> isWebp(header);
                default -> false;
            };
        } catch (IOException e) {
            return false;
        }
    }

    private boolean isPng(byte[] h) {
        byte[] sig = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (h.length < sig.length) return false;
        for (int i = 0; i < sig.length; i++) if (h[i] != sig[i]) return false;
        return true;
    }

    private boolean isJpeg(byte[] h) {
        return h[0] == (byte) 0xFF && h[1] == (byte) 0xD8 && h[2] == (byte) 0xFF;
    }

    private boolean isGif(byte[] h) {
        return h[0] == 0x47 && h[1] == 0x49 && h[2] == 0x46 && h[3] == 0x38;
    }

    private boolean isWebp(byte[] h) {
        if (h.length < 12) return false;
        return h[0] == 0x52 && h[1] == 0x49 && h[2] == 0x46 && h[3] == 0x46
                && h[8] == 0x57 && h[9] == 0x45 && h[10] == 0x42 && h[11] == 0x50;
    }
}

