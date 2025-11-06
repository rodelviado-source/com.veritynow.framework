package com.veritynow.core.api.bdo.personal.loans;


import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = { "http://localhost:5173" }, allowCredentials = "true")
public class ImagesController {

 private final ImageRepository imageRepository; // your service to load images by id
 private final RecordRepository recordRepository;

 public ImagesController(ImageRepository imageRepository, RecordRepository recordRepository) {
     this.imageRepository = imageRepository;
     this.recordRepository = recordRepository;
 }

 @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<java.util.Map<String, String>> uploadForImages(@RequestParam("file") MultipartFile file)
			throws Exception {
		String id = java.util.UUID.randomUUID().toString();
		ImageEntity img = new ImageEntity();
		img.setId(id);
		img.setFilename(file.getOriginalFilename());
		img.setSize(file.getSize());
		img.setContentType(
				file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType());
		img.setData(file.getBytes());
		imageRepository.save(img);
		return ResponseEntity.ok(java.util.Map.of("id", id, "contentType", img.getContentType()));
	}
 
 @PostMapping(value = "/records/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> uploadForRecord(@PathVariable Long id, @RequestParam("files") MultipartFile[] files) {
		return recordRepository.findById(id).map(r -> {
			for (MultipartFile f : files) {
				try {
					String imgId = java.util.UUID.randomUUID().toString();
					ImageEntity img = new ImageEntity();
					img.setId(imgId);
					img.setFilename(f.getOriginalFilename());
					img.setContentType(
							f.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : f.getContentType());
					img.setData(f.getBytes());
					img.setSize(f.getSize());
					imageRepository.save(img);
					r.getImageIds().add(imgId);
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
			recordRepository.save(r);
			return ResponseEntity.ok(java.util.Map.of("imageIds", r.getImageIds()));
		}).orElse(ResponseEntity.notFound().build());
	}
 
 // --- Stream the binary (you probably already have something like this) ---
 @GetMapping("/{id}")
 public ResponseEntity<? extends Resource> get(@PathVariable String id,
     @RequestHeader(value = "Range", required = false) String range) {
   return imageRepository.findById(id).map(img -> {
     byte[] data = img.getData();
     MediaType ct = parseMediaTypeOrDefault(img.getContentType(), img.getFilename());
 
     if (range != null && range.startsWith("bytes=")) {
       String[] p = range.substring(6).split("-", 2);
       long start = 0, end = data.length - 1;
       try {
         if (!p[0].isBlank()) start = Long.parseLong(p[0]);
         if (p.length > 1 && !p[1].isBlank()) end = Long.parseLong(p[1]);
       } catch (NumberFormatException ignored) {}
       start = Math.max(0, Math.min(start, data.length - 1));
       end   = Math.max(start, Math.min(end,   data.length - 1));
       byte[] slice = java.util.Arrays.copyOfRange(data, (int) start, (int) (end + 1));
 
       return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
            .contentType(ct)
            .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + data.length)
            .contentLength(slice.length)
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDispositionInline(img.getFilename()))
            .headers(h -> {
              h.setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
              h.setETag("\"" + img.getId() + ":" + img.getSize() + "\"");
           })
            .body(new ByteArrayResource(slice));
      }
 
      return ResponseEntity.ok()
          .contentType(ct)
          .contentLength(img.getSize())
          .header(HttpHeaders.CONTENT_DISPOSITION, contentDispositionInline(img.getFilename()))
          .headers(h -> {
            h.setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
            h.setETag("\"" + img.getId() + ":" + img.getSize() + "\"");
          })
          .body(new ByteArrayResource(data));
    }).orElse(ResponseEntity.notFound().build());
  }

 
 // --- HEAD: same headers, no body (so the client can choose a viewer fast) ---
 @RequestMapping(value = "/{id}", method = RequestMethod.HEAD)
 public ResponseEntity<?> head(@PathVariable String id) {
	 return imageRepository.findById(id)
		      .map(img -> ResponseEntity.ok()
		          .contentType(parseMediaTypeOrDefault(img.getContentType(), img.getFilename()))
		          .contentLength(img.getSize())
		          .header(HttpHeaders.CONTENT_DISPOSITION, contentDispositionInline(img.getFilename()))
		          .headers(h -> {
		        	  h.setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
		        	  h.setETag("\"" + img.getId() + ":" + img.getSize() + "\"");
		          })
		          .build())
		      .orElse(ResponseEntity.notFound().build());
		}

 // --- JSON metadata for simple UI switching (recommended) ---
 @GetMapping("/{id}/meta")
 public ResponseEntity<ImageMetaDto> meta(@PathVariable String id) {
	  return imageRepository.findById(id)
	      .map(img -> {
	        var url = ServletUriComponentsBuilder.fromCurrentContextPath()
	            .path("/api/images/{id}").buildAndExpand(id).toUri().toString();
	        return ResponseEntity.ok(new ImageMetaDto(
	            img.getId(),
	            normalizeContentType(img.getContentType(), img.getFilename()),
	            img.getFilename(),
	            img.getSize(),
	            url));
	      })
	      .orElse(ResponseEntity.notFound().build());
	}

 // --- Optional: batch meta for lists/grids ---
 @GetMapping("/meta")
 public ResponseEntity<List<ImageMetaDto>> metaBatch(@RequestParam("ids") List<String> ids) {
   // 1) Load all existing entities in one go
   List<ImageEntity> found = imageRepository.findAllById(ids);

   // 2) Index by id for O(1) lookup; handle dup IDs and preserve order
     Map<String, ImageEntity> byId = found.stream().collect(Collectors.toMap(
         ImageEntity::getId,
         i -> i,
         (a, b) -> a,
         LinkedHashMap::new
     ));
   // 3) Walk the original ids to preserve order; skip missing
   List<ImageMetaDto> out = ids.stream()
       .map(byId::get)
       .filter(Objects::nonNull)
       .map(img -> {
         URI url = ServletUriComponentsBuilder.fromCurrentContextPath()
             .path("/api/images/{id}")
             .buildAndExpand(img.getId())
             .toUri();
         return new ImageMetaDto(
             img.getId(),
             normalizeContentType(img.getContentType(), img.getFilename()),
             img.getFilename(),
             img.getSize(),     // see note below
             url.toString()
         );
       })
       .collect(Collectors.toList());

   return ResponseEntity.ok(out);
 }
 
 @DeleteMapping("/records/{id}/{imageId}")
	public ResponseEntity<?> detach(@PathVariable Long id, @PathVariable String imageId) {
		return recordRepository.findById(id).map(r -> {
			r.getImageIds().removeIf(s -> s.equals(imageId));
			recordRepository.save(r);
			return ResponseEntity.noContent().build();
		}).orElse(ResponseEntity.notFound().build());
	}

 
 // ---------- helpers ----------
 private static String contentDispositionInline(String filename) {
     if (filename == null || filename.isBlank()) return "inline";
     ContentDisposition cd = ContentDisposition.inline().filename(filename).build();
     return cd.toString();
 }

 private static MediaType parseMediaTypeOrDefault(String contentType, String filename) {
     try {
         if (contentType != null && !contentType.isBlank()) {
             return MediaType.parseMediaType(contentType);
         }
     } catch (Exception ignored) {}
     // quick fallback by extension
     String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
     if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
     if (lower.matches(".*\\.(png|jpg|jpeg|gif|webp|tif|tiff)$")) return MediaType.parseMediaType("image/*");
     if (lower.matches(".*\\.(mp4|webm|ogg|mov)$")) return MediaType.parseMediaType("video/*");
     return MediaType.APPLICATION_OCTET_STREAM;
 }

 private static String normalizeContentType(String contentType, String filename) {
     try {
         if (contentType != null && !contentType.isBlank()) {
             return MediaType.parseMediaType(contentType).toString();
         }
     } catch (Exception ignored) {}
     // fallback from filename if DB contentType is empty
     return parseMediaTypeOrDefault(null, filename).toString();
 }

 // DTO
 public record ImageMetaDto(String id, String contentType, String filename, long size, String url) {}
}
