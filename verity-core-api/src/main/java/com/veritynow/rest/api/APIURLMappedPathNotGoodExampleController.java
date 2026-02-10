package com.veritynow.rest.api;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import com.veritynow.core.store.meta.BlobMeta;
import com.veritynow.core.store.meta.VersionMeta;
import com.veritynow.core.store.versionstore.PathUtils;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.HttpUtils;
import util.JSON;

@RestController
@RequestMapping("/api/**")
public class APIURLMappedPathNotGoodExampleController {

	private static final Logger LOGGER = LogManager.getLogger();
	
	private final String namespace;
	private final APIURLMappedPathNotGoodExampleService apiService;

	public APIURLMappedPathNotGoodExampleController(APIURLMappedPathNotGoodExampleService apiService, @Value("${verity.api.namespace:/vn}") String namespace) {
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

			try {
				 Optional<String> nameCDOpt = HttpUtils.extractFilenameFromHeaderContentDisposition(request);
				if (nameCDOpt.isPresent()) {
					name = nameCDOpt.get();
				}
			} catch (Exception e) {
				LOGGER.warn("Unable to extract filename in Header ContentDisposition", e);
			}

			// Here, request URI is the *collection* path, e.g. "/api/agents/A/clients"
			String parentPath = APIUtils.applyNamespace(request, namespace);
			String lastSegment = UUID.randomUUID().toString();
			parentPath = parentPath +"/" +lastSegment;
			
			Optional<VersionMeta> opt = apiService.createExactPath(parentPath, inputStream, contentType, name);
			
			if (opt.isPresent()) {
				VersionMeta vm = opt.get();
				return ResponseEntity.status(HttpStatus.CREATED).body(APIUtils.toClientVersionMeta(vm, namespace));
			}
					
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
			
		} catch (Exception e) {
			LOGGER.error("Create failed", e);
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

			
			try {
				 Optional<String> nameCDOpt = HttpUtils.extractFilenameFromHeaderContentDisposition(request);
				if (nameCDOpt.isPresent()) {
					name = nameCDOpt.get();
				}
			} catch (Exception e) {
				LOGGER.warn("Unable to extract filename in Header ContentDisposition", e);
			}

			// Here, request URI is the *collection* path, e.g. "/api/agents/A/clients/{id}"
			String parentPath = APIUtils.applyNamespace(request, namespace);
			
			 Optional<VersionMeta> opt = apiService.createExactPath(parentPath, inputStream, contentType, name);
			
			if (opt.isPresent()) {
				VersionMeta vm = opt.get();
			
				vm = APIUtils.toClientVersionMeta(vm, namespace);
				return ResponseEntity.status(HttpStatus.CREATED).body(vm);
			}
					
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
			
		} catch (Exception e) {
			LOGGER.error("Create exact path failed", e);
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

			try {
				 Optional<String> nameCDOpt = HttpUtils.extractFilenameFromHeaderContentDisposition(request);
				if (nameCDOpt.isPresent()) {
					name = nameCDOpt.get();
				}
			} catch (Exception e) {
				LOGGER.warn("Unable to extract filename in Header ContentDisposition", e);
			}

			// Here, request URI is the *identity* path, e.g.
			// "/api/agents/A/clients/{clientId}"
			String identityPath = APIUtils.applyNamespace(request, namespace);

			 Optional<VersionMeta> opt = apiService.update(identityPath, inputStream, contentType, name);
			
			if (opt.isPresent()) {
				VersionMeta vm = opt.get();
				return ResponseEntity.ok(APIUtils.toClientVersionMeta(vm, namespace));
			}
					
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
			
		} catch (Exception e) {
			LOGGER.error("Update failed", e);
			return ResponseEntity.internalServerError().build();
		}
		
	}

	

	@DeleteMapping
	public ResponseEntity<BlobMeta> delete(HttpServletRequest request, @RequestHeader HttpHeaders headers,
			@RequestParam(name = "reason", required = false) String reason) {

		try {
		String path = APIUtils.applyNamespace(request, namespace);

		Optional<BlobMeta> opt = apiService.delete(path, reason);
		if (opt.isPresent())
			return ResponseEntity.ok(opt.get());
		} catch (Exception e) {
			LOGGER.error("Delete failed", e);
			return ResponseEntity.internalServerError().build();
		}
		return ResponseEntity.badRequest().build();

	}
	
	@PutMapping(params = "undelete")
	public ResponseEntity<VersionMeta> undelete(HttpServletRequest request, @RequestHeader HttpHeaders headers
			) {

		try {
		String path = APIUtils.applyNamespace(request, namespace);
		 Optional<VersionMeta> opt = apiService.undelete(path);
		if (opt.isPresent()) {
			VersionMeta vm = opt.get();
			return ResponseEntity.ok(APIUtils.toClientVersionMeta(vm, namespace));
		}
		} catch (Exception e) {
			LOGGER.error("Undelete failed", e);
			return ResponseEntity.internalServerError().build();
		}
		return ResponseEntity.badRequest().build();
	}
	
	@PutMapping(params = "restore")
	public ResponseEntity<VersionMeta> restore(HttpServletRequest request, @RequestHeader HttpHeaders headers,
			 @RequestParam(value = "restore", required = true) String hash)
	 {
		
		try {
		String path = APIUtils.applyNamespace(request, namespace);

		 Optional<VersionMeta> opt = apiService.restore(path, hash);
		if (opt.isPresent()) {
			VersionMeta vm = opt.get();
			return ResponseEntity.ok(APIUtils.toClientVersionMeta(vm, namespace));
		}
		} catch (Exception e) {
			LOGGER.error("Restore failed", e);
			return ResponseEntity.internalServerError().build();
		}
		return ResponseEntity.badRequest().build();

	}

	
	
	
	
}
