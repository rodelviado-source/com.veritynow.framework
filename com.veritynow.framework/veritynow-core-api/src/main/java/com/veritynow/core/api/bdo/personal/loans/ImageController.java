package com.veritynow.core.api.bdo.personal.loans;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = { "http://localhost:5173" }, allowCredentials = "true")
public class ImageController {
	private final ImageService svc;
	private final RecordRepository records;

	public ImageController(ImageService svc, RecordRepository records) {
		this.svc = svc;
		this.records = records;
	}

	@GetMapping("/{imageId}")
	public ResponseEntity<byte[]> get(@PathVariable String imageId) {
		ImageEntity img = svc.get(imageId);
		if (img == null || img.getData() == null)
			return ResponseEntity.notFound().build();
		String ct = img.getContentType() == null ? MediaType.IMAGE_PNG_VALUE : img.getContentType();
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(ct)).body(img.getData());
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<java.util.Map<String, String>> upload(@RequestParam("file") MultipartFile file)
			throws Exception {
		String id = java.util.UUID.randomUUID().toString();
		ImageEntity img = new ImageEntity();
		img.setId(id);
		img.setContentType(
				file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType());
		img.setData(file.getBytes());
		svc.save(img);
		return ResponseEntity.ok(java.util.Map.of("id", id, "contentType", img.getContentType()));
	}

	@PostMapping(value = "/records/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> uploadForRecord(@PathVariable Long id, @RequestParam("files") MultipartFile[] files) {
		return records.findById(id).map(r -> {
			for (MultipartFile f : files) {
				try {
					String imgId = java.util.UUID.randomUUID().toString();
					ImageEntity img = new ImageEntity();
					img.setId(imgId);
					img.setContentType(
							f.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : f.getContentType());
					img.setData(f.getBytes());
					svc.save(img);
					r.getImageIds().add(imgId);
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
			records.save(r);
			return ResponseEntity.ok(java.util.Map.of("imageIds", r.getImageIds()));
		}).orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/records/{id}/{imageId}")
	public ResponseEntity<?> detach(@PathVariable Long id, @PathVariable String imageId) {
		return records.findById(id).map(r -> {
			r.getImageIds().removeIf(s -> s.equals(imageId));
			records.save(r);
			return ResponseEntity.noContent().build();
		}).orElse(ResponseEntity.notFound().build());
	}
}