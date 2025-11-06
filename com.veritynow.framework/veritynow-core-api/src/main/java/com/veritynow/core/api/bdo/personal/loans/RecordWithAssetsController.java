package com.veritynow.core.api.bdo.personal.loans;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/records-with-assets")
@CrossOrigin(origins = { "http://localhost:5173" }, allowCredentials = "true")
public class RecordWithAssetsController {

  private final RecordRepository recordRepository;
  private final ImageRepository imageRepository;

  public RecordWithAssetsController(RecordRepository recordRepository, ImageRepository imageRepository) {
    this.recordRepository = recordRepository;
    this.imageRepository = imageRepository;
  }

  /**
   * Create a record and its images in one shot.
   * Expects multipart/form-data with:
   * - part "record": JSON body mirroring your CreateRecordRequest (same fields as /api/records)
   * - part "files": 0..N files
   */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CreateWithAssetsResponse> createWithAssets(
      @RequestPart("record") CreateRecordRequest req,
      @RequestPart(name = "files", required = false) List<MultipartFile> files) throws Exception {

    // Business constraint: unique (agentId, clientId), same as your create() endpoint
    if (recordRepository.existsByAgentIdAndClientId(req.agentId, req.clientId)) {
      return ResponseEntity.badRequest().build();
    }

    // --- Build & save the RecordEntity (same mapping as your /api/records create)
    RecordEntity r = new RecordEntity();
    r.setAgentId(req.agentId);
    r.setAgentFirstName(req.agentFirstName);
    r.setAgentMiddleName(req.agentMiddleName);
    r.setAgentLastName(req.agentLastName);
    r.setAgentSuffix(req.agentSuffix);

    r.setClientId(req.clientId);
    r.setClientFirstName(req.clientFirstName);
    r.setClientMiddleName(req.clientMiddleName);
    r.setClientLastName(req.clientLastName);
    r.setClientSuffix(req.clientSuffix);

    r.setTitle(req.title);
    r.setPriority(req.priority);
    r.setStatus(req.status);
    r.setDescription(req.description);
    r.setCreatedAt(req.createdAt != null ? req.createdAt : Instant.now());

    r = recordRepository.save(r);

    // --- Persist files (same logic/style as ImagesController /records/{id})
    List<String> imageIds = new ArrayList<>();
    if (files != null) {
      for (MultipartFile f : files) {
        if (f == null || f.isEmpty()) continue;

        String imgId = UUID.randomUUID().toString();
        ImageEntity img = new ImageEntity();
        img.setId(imgId);
        img.setFilename(f.getOriginalFilename());
        img.setContentType(f.getContentType() == null
            ? MediaType.APPLICATION_OCTET_STREAM_VALUE
            : f.getContentType());
        img.setData(f.getBytes());
        img.setSize(f.getSize());
        imageRepository.save(img);

        r.getImageIds().add(imgId); // keep IDs in the record’s collection
        imageIds.add(imgId);
      }
      recordRepository.save(r);
    }

    return ResponseEntity.ok(new CreateWithAssetsResponse(r.getId(), imageIds));
  }

  // Response DTO
  public record CreateWithAssetsResponse(Long id, List<String> imageIds) {}
}
