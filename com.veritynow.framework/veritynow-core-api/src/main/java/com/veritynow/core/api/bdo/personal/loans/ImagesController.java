package com.veritynow.core.api.bdo.personal.loans;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = {"http://localhost:5173","http://127.0.0.1:5173"})
public class ImagesController {

  private final ImageService imageService;
  private final RecordRepository recordRepository;

  public ImagesController(ImageService imageService, RecordRepository recordRepository) {
    this.imageService = imageService;
    this.recordRepository = recordRepository;
  }

  // Upload multiple images and associate to a record
  @PostMapping("/records/{recordId}")
  public ResponseEntity<Map<String, Object>> upload(
      @PathVariable Long recordId,
      @RequestParam("files") List<MultipartFile> files
  ) throws IOException {

    var rec = recordRepository.findById(recordId).orElse(null);
    if (rec == null) return ResponseEntity.notFound().build();

    List<String> ids = new ArrayList<>();
    for (MultipartFile f : files) {
      if (f.isEmpty()) continue;
      ImageEntity img = new ImageEntity();
      img.setId(UUID.randomUUID().toString());
      img.setContentType(Optional.ofNullable(f.getContentType()).orElse("application/octet-stream"));
      img.setData(f.getBytes());
      imageService.save(img);
      ids.add(img.getId());
    }

    // Append to record’s imageIds (if your entity has it); persist record if needed
    rec.getImageIds().addAll(ids);
    recordRepository.save(rec);

    return ResponseEntity.ok(Map.of("imageIds", rec.getImageIds()));
  }

  // Stream single image by id
  @GetMapping("/{id}")
  public ResponseEntity<byte[]> get(@PathVariable String id) {
    return imageService.find(id)
        .map(img -> {
          HttpHeaders h = new HttpHeaders();
          h.setContentType(MediaType.parseMediaType(
              Optional.ofNullable(img.getContentType()).orElse("application/octet-stream")));
          h.setCacheControl(CacheControl.noCache());
          return new ResponseEntity<>(img.getData(), h, HttpStatus.OK);
        })
        .orElse(ResponseEntity.notFound().build());
  }
}
