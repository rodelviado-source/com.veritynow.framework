package com.veritynow.v2.api;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.veritynow.v2.store.meta.BlobMeta;
import com.veritynow.v2.store.meta.VersionMeta;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.HttpUtils;

@RestController
@RequestMapping("/api/**")
public class APIController {

	private final String namespace;
	private final APIService apiService;

	public APIController(APIService apiService, @Value("${verity.api.namespace:/vn}") String namespace) {
		this.apiService = apiService;
		this.namespace = PathUtils.normalizeNamespace(namespace);
	}

	@PutMapping(params = "create")
	public ResponseEntity<VersionMeta> create(HttpServletRequest request,
			@RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
			@RequestHeader(value = HttpHeaders.CONTENT_DISPOSITION, required = false) String contentDisposition,
			@RequestHeader HttpHeaders headers) {

		try (InputStream inputStream = request.getInputStream()) {
			String name = "blob";

			String nameCD = HttpUtils.extractFilenameFromHeaderContentDisposition(request);
			if (nameCD != null && !nameCD.isBlank()) {
				name = nameCD;
			}

			// Here, request URI is the *collection* path, e.g. "/api/agents/A/clients"
			String parentPath = PathUtils.applyNamespace(request, namespace);
			String lastSegment = UUID.randomUUID().toString();
			parentPath = parentPath +"/" +lastSegment;
			
			Optional<VersionMeta> opt = apiService.createExactPath(parentPath, inputStream, contentType, name);
			
			if (opt.isPresent()) {
				VersionMeta vm = opt.get();
				return ResponseEntity.status(HttpStatus.CREATED).body(PathUtils.toClientVersionMeta(vm, namespace));
			}
					
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
			
		} catch (Exception e) {
			return ResponseEntity.internalServerError().build();
		}
	}
	
	@PutMapping(params = {"create", "exactPath"})
	public ResponseEntity<VersionMeta> createWithId(HttpServletRequest request,
			@RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
			@RequestHeader(value = HttpHeaders.CONTENT_DISPOSITION, required = false) String contentDisposition,
			@RequestHeader HttpHeaders headers) {

		try (InputStream inputStream = request.getInputStream()) {
			String name = "blob";

			
			String nameCD = HttpUtils.extractFilenameFromHeaderContentDisposition(request);
			
			if (nameCD != null && !nameCD.isBlank()) {
				name = nameCD;
			}

			// Here, request URI is the *collection* path, e.g. "/api/agents/A/clients/{id}"
			String parentPath = PathUtils.applyNamespace(request, namespace);
			
			 Optional<VersionMeta> opt = apiService.createExactPath(parentPath, inputStream, contentType, name);
			
			if (opt.isPresent()) {
				VersionMeta vm = opt.get();
			
				vm = PathUtils.toClientVersionMeta(vm, namespace);
				return ResponseEntity.status(HttpStatus.CREATED).body(vm);
			}
					
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
			
		} catch (Exception e) {
			return ResponseEntity.internalServerError().build();
		}
	}
	
	

	@PutMapping
	public ResponseEntity<VersionMeta> update(HttpServletRequest request,
			@RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
			@RequestHeader(value = HttpHeaders.CONTENT_DISPOSITION, required = false) String contentDisposition,
			@RequestHeader HttpHeaders headers) {

		try (InputStream inputStream = request.getInputStream()) {
			String name = "blob";

			String nameCD = HttpUtils.extractFilenameFromHeaderContentDisposition(request);
			if (nameCD != null && !nameCD.isBlank()) {
				name = nameCD;
			}

			// Here, request URI is the *identity* path, e.g.
			// "/api/agents/A/clients/{clientId}"
			String identityPath = PathUtils.applyNamespace(request, namespace);

			 Optional<VersionMeta> opt = apiService.update(identityPath, inputStream, contentType, name);
			
			if (opt.isPresent()) {
				VersionMeta vm = opt.get();
				return ResponseEntity.ok(PathUtils.toClientVersionMeta(vm, namespace));
			}
					
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
			
		} catch (Exception e) {
			return ResponseEntity.internalServerError().build();
		}
		
	}

		
	
	@GetMapping (produces = { MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE})
	public void get(HttpServletRequest request, HttpServletResponse response) {

		String path = PathUtils.applyNamespace(request, namespace);
		Optional<InputStream> opt = apiService.get(path);
		if (opt.isEmpty()) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return;
		}
		
		try (ServletOutputStream sos = response.getOutputStream(); InputStream payload = opt.get();) {
			//response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
			
			StreamUtils.copy(payload, sos);
			response.setStatus(HttpStatus.OK.value());
	        response.flushBuffer();
		} catch (Exception e) {
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
	}
	



	@DeleteMapping
	public ResponseEntity<BlobMeta> delete(HttpServletRequest request, @RequestHeader HttpHeaders headers,
			@RequestParam(name = "reason", required = false) String reason) {

		String path = PathUtils.applyNamespace(request, namespace);

		Optional<BlobMeta> opt = apiService.delete(path, reason);
		if (opt.isPresent())
			return ResponseEntity.ok(opt.get());
		
		return ResponseEntity.badRequest().build();

	}
	
	@PutMapping(params = "undelete")
	public ResponseEntity<VersionMeta> undelete(HttpServletRequest request, @RequestHeader HttpHeaders headers
			) {

		String path = PathUtils.applyNamespace(request, namespace);
		 Optional<VersionMeta> opt = apiService.undelete(path);
		if (opt.isPresent()) {
			VersionMeta vm = opt.get();
			return ResponseEntity.ok(PathUtils.toClientVersionMeta(vm, namespace));
		}
		return ResponseEntity.badRequest().build();
	}
	
	@PutMapping(params = "restore")
	public ResponseEntity<VersionMeta> restore(HttpServletRequest request, @RequestHeader HttpHeaders headers,
			 @RequestParam(value = "restore", required = true) String hash)
	 {
		System.out.println(hash);
		String path = PathUtils.applyNamespace(request, namespace);

		 Optional<VersionMeta> opt = apiService.restore(path, hash);
		if (opt.isPresent()) {
			VersionMeta vm = opt.get();
			return ResponseEntity.ok(PathUtils.toClientVersionMeta(vm, namespace));
		}
		return ResponseEntity.badRequest().build();

	}

	@GetMapping(params = "list")
	public ResponseEntity<List<VersionMeta>> list(HttpServletRequest request, @RequestHeader HttpHeaders headers) {
		String path = PathUtils.applyNamespace(request, namespace);
		 List<VersionMeta> metas = apiService.list(path);
		List<VersionMeta> clientMetas = metas.stream().map((vm) -> {return PathUtils.toClientVersionMeta(vm, namespace);}).toList();
		return ResponseEntity.ok(clientMetas);

	}
	
	
}
