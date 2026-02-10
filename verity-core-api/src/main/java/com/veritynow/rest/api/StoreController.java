package com.veritynow.rest.api;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.veritynow.core.context.Context;
import com.veritynow.core.context.ContextResolvers;
import com.veritynow.core.context.ContextScope;
import com.veritynow.core.context.ContextSnapshot;
import com.veritynow.core.store.base.PathEvent;
import com.veritynow.core.store.meta.BlobMeta;
import com.veritynow.core.store.meta.PathMeta;
import com.veritynow.core.store.meta.VersionMeta;
import com.veritynow.core.store.versionstore.PathUtils;

import jakarta.servlet.http.HttpServletRequest;
import util.HttpUtils;
import util.JSON;
import util.StringUtils;

@RestController
public class StoreController {
	
	private final StoreService storeService;
	private static final Logger LOGGER = LogManager.getLogger();

	private final String namespace;

	public StoreController(StoreService consoleService, @Value("${verity.api.namespace:/vn}") String namespace) {
		this.storeService = consoleService;
		this.namespace = PathUtils.normalizeNamespace(namespace);
	}
	
	/**
	 * Retrieve the content of the blob via its hash 
	 * 
	 * @param request - the hash of the blob's content 
	 * @return the InputStream content of the blob addressed by its hash 
	 */
	@PostMapping(value = "/api/read/content", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<InputStreamResource> getContent(@RequestBody Map<String, String> request) {

		Optional<InputStream> opt;
		String hash = null;
		try {
			hash  = request.get("hash");
			Objects.requireNonNull(hash, "hash");
			
			opt = storeService.getContent(hash);
			if (opt.isEmpty())
				return ResponseEntity.notFound().build();

			Optional<BlobMeta> bm = storeService.getContentMeta(hash);
			 MediaType contentMimeType = MediaType.APPLICATION_OCTET_STREAM;
			if (bm.isPresent()) {
				String mt = bm.get().mimeType();
				if (mt != null) contentMimeType = MediaType.parseMediaType(mt);
			}

			HttpHeaders h = new HttpHeaders();
			h.setContentType(contentMimeType);
			return ResponseEntity.ok().headers(h).body(new InputStreamResource(opt.get()));
			
		} catch (Exception e) {
			LOGGER.error("Failed to get content hash={}", hash,e);
			return ResponseEntity.internalServerError().build();
		}
		
	}
	
	@PostMapping(path="/api/read/children/latest/version",
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<VersionMeta>> getChildrenLatestVersion(@RequestBody Map<String, String> request) {
		try {
		String path = request.get("path");
		Objects.requireNonNull(path, "path");
		String storePath = PathUtils.normalizeAndApplyNamespace(path, namespace);
	     List<VersionMeta> vms = storeService.getChildrenLatestVersion(storePath);
		List<VersionMeta> clientVms = vms.stream().map((vm) -> {return APIUtils.toClientVersionMeta(vm, namespace);}).toList();
		return ResponseEntity.ok(clientVms);
		} catch (Exception e) {
			LOGGER.error("getChildrenLatestVersion failed", e);
			return ResponseEntity.internalServerError().build();
		}
		
	}
	
	@PostMapping(path="/api/read/latest/version",
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<VersionMeta> getLatestVersion(@RequestBody Map<String, String> request) {
		try {
		String path = request.get("path");
		Objects.requireNonNull(path, "path");
		
		String storePath = PathUtils.normalizeAndApplyNamespace(path, namespace);
		 
		Optional<VersionMeta> vm = storeService.getLatestVersion(storePath);
		if (vm.isPresent()) {
			VersionMeta vmc = APIUtils.toClientVersionMeta(vm.get(), namespace);
			return ResponseEntity.ok(vmc);
		}
		
		return ResponseEntity.ok(null);
		
		} catch (Exception e) {
			LOGGER.error("getLatestVersion failed", e);
			return ResponseEntity.internalServerError().build();
		}
	}
	
	@PostMapping(path="/api/read/all/versions", 
			consumes = MediaType.APPLICATION_JSON_VALUE, 
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<VersionMeta>> getAllVersion(@RequestBody Map<String, String> request) {
		try {
		String path = request.get("path");
		Objects.requireNonNull(path, "path");
		
		String storePath = PathUtils.normalizeAndApplyNamespace(path, namespace);
		 
		List<VersionMeta> vms = storeService.getAllVersions(storePath);
		List<VersionMeta> clientVms = vms.stream().map((vm) -> {return APIUtils.toClientVersionMeta(vm, namespace);}).toList();
		
		return ResponseEntity.ok(clientVms);
		
		} catch (Exception e) {
			LOGGER.error("getAllVersions failed", e);
			return ResponseEntity.internalServerError().build();
		}
	}
	
	
	@PostMapping(path="/api/read/blob/content/latest/version",
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = { MediaType.APPLICATION_OCTET_STREAM_VALUE, 
					    MediaType.APPLICATION_JSON_VALUE} )
	public ResponseEntity<InputStreamResource> getBlobContentLatestVersion(@RequestBody Map<String, String> request) {
		try {
		String path = request.get("path");
		Objects.requireNonNull(path, "path");
		
		String storePath = PathUtils.normalizeAndApplyNamespace(path, namespace);
		 
		Optional<VersionMeta> vmOpt = storeService.getLatestVersion(storePath);
		if (vmOpt.isPresent()) {
			VersionMeta vm = vmOpt.get();
			Optional<InputStream> isOpt = storeService.getContent(vm.hash());
			if (isOpt.isEmpty())
				return ResponseEntity.notFound().build();

			Optional<BlobMeta> bm = storeService.getContentMeta(vm.hash());
			MediaType contentMimeType = MediaType.APPLICATION_OCTET_STREAM;
			if (bm.isPresent()) {
				String mt = bm.get().mimeType();
				if (mt != null) contentMimeType = MediaType.parseMediaType(mt);
			}

			HttpHeaders h = new HttpHeaders();
			h.setContentType(contentMimeType);
			return ResponseEntity.ok().headers(h).body(new InputStreamResource(isOpt.get()));
		}
		
		return ResponseEntity.ok(null);
		
		} catch (Exception e) {
			LOGGER.error("getBlobLatestVersion failed", e);
			return ResponseEntity.internalServerError().build();
		}
	}
	
	@PostMapping(path = "/api/read/path/meta", 
			   consumes = MediaType.APPLICATION_JSON_VALUE, 
			   produces = MediaType.APPLICATION_JSON_VALUE )
	public ResponseEntity<PathMeta> meta(@RequestBody Map<String, String> request) {

		String path = null;
		
		try {

			path = request.get("path");
			Objects.requireNonNull(path, "path");
			
			String storePath = PathUtils.normalizeAndApplyNamespace(path, namespace);
			

			Optional<PathMeta> opt = storeService.getPathMeta(storePath);
			if (opt.isEmpty())
				return ResponseEntity.notFound().build();

			// Strip namespace for EVERYTHING returned to the client

			PathMeta m = opt.get();
			String fixedPath = PathUtils.removeNamespace(m.path(), namespace);
			List<String> fixedChildren = m.children();

			if (fixedChildren != null) {
				fixedChildren = m.children().stream().map((s) -> PathUtils.lastSegment(s)).toList();
			}

			List<VersionMeta> fixedVersions = m.versions().stream().map((v) -> {
				String vmPath = PathUtils.removeNamespace(v.path(), namespace);
				return new VersionMeta(v.blobMeta(), new PathEvent(vmPath, v.timestamp(), v.operation(), v.principal(),
						v.correlationId(), v.workflowId(), v.contextName(), v.transactionId(), v.transactionResult()));

			}).toList();

			PathMeta mx = new PathMeta(fixedPath, fixedChildren, fixedVersions);

			return ResponseEntity.ok(mx);
		} catch (Exception e) {
			LOGGER.error("Unable to get meta for {}", path, e);
		}
		return ResponseEntity.badRequest().build();
	}
	

	@PostMapping(path="/api/processor", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<VersionMeta> create(
			@RequestPart("operation") String operation,
			@RequestPart("path") String path,
			@RequestPart("hash") String hash,
            @RequestPart("file") MultipartFile file   
			) {

		try (InputStream inputStream = file.getInputStream()) {
			String name = StringUtils.isEmpty(file.getOriginalFilename())  ? 
					"blob": file.getOriginalFilename();
			
			String contentType = StringUtils.isEmpty(file.getContentType()) ? 
					MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType();
			
			String lastSegment = UUID.randomUUID().toString();
			String storePath = path +"/" +lastSegment;
			
			storePath = PathUtils.normalizeAndApplyNamespace(storePath, namespace);
			
			Optional<VersionMeta> opt = storeService.createExactPath(storePath, inputStream, contentType, name);
			
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
	
	
	@PostMapping(path = "/api/txn/processor", 
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE, 
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> txnMultipart(@RequestPart("transactions") String transactionsJson,
			MultipartHttpServletRequest request) throws Exception {

		List<Transaction> txns = JSON.MAPPER.readValue(transactionsJson, new TypeReference<List<Transaction>>() {
		});

		// Exact multipart parts as uploaded: partName -> MultipartFile
		Map<String, MultipartFile> fileMap = request.getFileMap(); // Map<String, MultipartFile>

		// for your debugging response
		List<String> available = new ArrayList<>(fileMap.keySet());

		// Verify blobRefs exist (your current error logic)
		for (Transaction t : txns) {
			String ref = t.blobRef();
			if (ref != null && !ref.isBlank()) {
				if (!fileMap.containsKey(ref)) {
					return ResponseEntity.badRequest().body(
							Map.of("blobRef", ref, "available", available, "error",
							"Transaction.blobRef not found among uploaded blobs"));
				}
			}
		}

		// Build your APITransaction (InputStreams are per-file)
		java.util.Map<String, InputStream> blobs = new LinkedHashMap<>();
		for (var e : fileMap.entrySet()) {
			blobs.put(e.getKey(), e.getValue().getInputStream());
		}
		
		List<Transaction> namespacetxns = new ArrayList<>();
		
		for (Transaction t : txns) {
			namespacetxns.add(new Transaction(
				PathUtils.normalizeAndApplyNamespace(t.path(), namespace),
				t.blobRef(),
				t.blobMimetype(),
				t.operation()
			));
		}
		
		APITransaction apiTxn = new APITransaction(namespacetxns, blobs);

		// ---- TEST HARNESS PLACEHOLDER ----
		// Consume streams here (or pass MultipartFile instead).
		// Example: just return a summary to prove wiring works.
		Map<String, Object> summary = new LinkedHashMap<>();
		summary.put("transactionCount", apiTxn.transactions().size());
		summary.put("blobCount", apiTxn.blobs().size());
		summary.put("blobs", apiTxn.blobs().keySet());

		//get context from headers if any
		//if transactionId was not provided use the provided UUID
		ContextSnapshot ctx = ContextResolvers.fromHttpHeaders(
				request.getRequestHeaders().toSingleValueMap(),
				UUID.randomUUID().toString()
		);
		
		try (ContextScope scope = Context.scope(ctx)) {
			
			storeService.processTransaction(apiTxn, fileMap);
		} finally {
			// Cleanup to avoid multipart stream leaks
			for (InputStream is : apiTxn.blobs().values()) {
				try (is) {
				} catch (Exception ignored) {
				}
			}
		}

		return ResponseEntity.ok(summary);
	}

}
