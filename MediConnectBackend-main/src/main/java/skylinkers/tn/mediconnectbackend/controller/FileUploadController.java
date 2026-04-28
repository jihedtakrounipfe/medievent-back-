package skylinkers.tn.mediconnectbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import skylinkers.tn.mediconnectbackend.exception.FileUploadException;
import skylinkers.tn.mediconnectbackend.service.FileUploadService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService uploadService;

    @PostMapping("/upload")
    public ResponseEntity<FileUploadService.UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        FileUploadService.UploadResponse stored = uploadService.uploadImage(file, null);
        String absoluteUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(stored.url())
                .toUriString();
        return ResponseEntity.ok(new FileUploadService.UploadResponse(absoluteUrl));
    }

    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> image(@PathVariable String filename) {
        Path p = uploadService.resolveImage(filename);
        if (!Files.exists(p)) {
            throw new FileUploadException("File not found.");
        }
        try {
            Resource resource = new UrlResource(p.toUri());
            String contentType = Files.probeContentType(p);
            MediaType mt = contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
            return ResponseEntity.ok()
                    .contentType(mt)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new FileUploadException("Invalid file path.", e);
        } catch (IOException e) {
            throw new FileUploadException("Unable to read file.", e);
        }
    }
}
