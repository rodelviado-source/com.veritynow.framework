package com.veritynow.core.utils;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FileUploadController {

  private final Path uploadDir = Paths.get("uploads");

  public FileUploadController() throws IOException {
    if (!Files.exists(uploadDir)) {
      Files.createDirectories(uploadDir);
    }
  }

  
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Map<String, Object>> upload(@RequestPart("file") MultipartFile file) throws IOException {
    if (file.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Empty file"));
    }
    String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
    String storedName = UUID.randomUUID() + "-" + original;
    Path target = uploadDir.resolve(storedName);
    Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

    return ResponseEntity.ok(Map.of(
        "filename", original,
        "storedName", storedName,
        "size", file.getSize(),
        "savedTo", target.toAbsolutePath().toString(),
        "timestamp", OffsetDateTime.now().toString()
    ));
  }
  
  
  @GetMapping("/upload/ping")
  public Map<String, String> ping() {
      return Map.of("status", "ok");
  }
  
  
}
