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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.veritynow.core.context.Context;
import com.veritynow.core.context.ContextResolvers;
import com.veritynow.core.context.ContextScope;
import com.veritynow.core.context.ContextSnapshot;
import com.veritynow.core.store.meta.BlobMeta;
import com.veritynow.core.store.meta.PathMeta;
import com.veritynow.core.store.meta.VersionMeta;
import com.veritynow.core.store.versionstore.PathUtils;
import com.veritynow.util.JSON;
import com.veritynow.util.StringUtils;

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
	 * Read raw blob bytes by content hash.
	 *
	 * <p>Request body:</p>
	 * <pre>{@code
	 * { "hash": "<content-hash>" }
	 * }</pre>
	 *
	 * <p>Responses:</p>
	 * <ul>
	 *   <li><b>200</b> with the blob bytes (Content-Type derived from stored {@link BlobMeta} when available; otherwise
	 *       {@code application/octet-stream}).</li>
	 *   <li><b>404</b> if the hash is unknown.</li>
	 * </ul>
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
	
	/**
 * Read stored {@link BlobMeta} by content hash.
 *
 * <p>Request body:</p>
 * <pre>{@code
 * { "hash": "<content-hash>" }
 * }</pre>
 *
 * <p>Responses:</p>
 * <ul>
 *   <li><b>200</b> with {@link BlobMeta}.</li>
 *   <li><b>404</b> if the hash is unknown.</li>
 * </ul>
 */
	@PostMapping(value = "/api/read/content/meta", 
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<BlobMeta> getContentMeta(@RequestBody Map<String, String> request) {

		String hash = null;
		try {
			hash  = request.get("hash");
			Objects.requireNonNull(hash, "hash");

			Optional<BlobMeta> bm = storeService.getContentMeta(hash);
			if (bm.isPresent()) {
				return ResponseEntity.ok(bm.get());
			}
			return ResponseEntity.notFound().build();
		} catch (Exception e) {
			LOGGER.error("Failed to get content hash={}", hash,e);
			return ResponseEntity.internalServerError().build();
		}
		
	}
	
	/**
 * Read the latest (HEAD) {@link VersionMeta} for each <em>direct</em> child of a path.
 *
 * <p>This endpoint is query-like: it always returns a JSON array (possibly empty) and never {@code null}.</p>
 *
 * <p>Request body:</p>
 * <pre>{@code
 * { "path": "<identity-path>" }
 * }</pre>
 *
 * <p>Responses:</p>
 * <ul>
 *   <li><b>200</b> with {@code List<VersionMeta>} (empty if the path has no children or does not exist).</li>
 * </ul>
 */
	@PostMapping(path="/api/read/children/latest/version",
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<VersionMeta>> getChildrenLatestVersion(@RequestBody Map<String, String> request) {
		try {
		String path = request.get("path");
		Objects.requireNonNull(path, "path");
		String storePath = PathUtils.normalizeAndApplyNamespace(path, namespace);
	     List<VersionMeta> vms = storeService.getChildrenLatestVersion(storePath);
		List<VersionMeta> clientVms = APIUtils.toClientVersionMeta(vms, namespace);
		return ResponseEntity.ok(clientVms);
		} catch (Exception e) {
			LOGGER.error("getChildrenLatestVersion failed", e);
			return ResponseEntity.internalServerError().build();
		}
		
	}
	
	/**
 * Read the direct child path segments (names) of a path.
 *
 * <p>This endpoint is query-like: it always returns a JSON array (possibly empty) and never {@code null}.</p>
 *
 * <p>Request body:</p>
 * <pre>{@code
 * { "path": "<identity-path>" }
 * }</pre>
 *
 * <p>Responses:</p>
 * <ul>
 *   <li><b>200</b> with {@code List<String>} (empty if the path has no children or does not exist).</li>
 * </ul>
 */
	@PostMapping(path="/api/read/children/path",
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<String>> getChildrenPath(@RequestBody Map<String, String> request) {
		try {
		String path = request.get("path");
		Objects.requireNonNull(path, "path");
		String storePath = PathUtils.normalizeAndApplyNamespace(path, namespace);
	     List<String> paths = storeService.getChildrenPath(storePath);
		return ResponseEntity.ok(paths);
		} catch (Exception e) {
			LOGGER.error("getChildrenLatestPath failed", e);
			return ResponseEntity.internalServerError().build();
		}
		
	}
	
	/**
 * Read the latest (HEAD) {@link VersionMeta} for a path.
 *
 * <p>Request body:</p>
 * <pre>{@code
 * { "path": "<identity-path>" }
 * }</pre>
 *
 * <p>Responses (Optional semantics):</p>
 * <ul>
 *   <li><b>200</b> with {@link VersionMeta} if present.</li>
 *   <li><b>404</b> if the path does not exist or has no versions.</li>
 * </ul>
 */
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
		
		return ResponseEntity.notFound().build();
		
		} catch (Exception e) {
			LOGGER.error("getLatestVersion failed", e);
			return ResponseEntity.internalServerError().build();
		}
	}
	
	/**
 * Read all versions for a path (newest first).
 *
 * <p>This endpoint is query-like: it always returns a JSON array (possibly empty) and never {@code null}.</p>
 *
 * <p>Request body:</p>
 * <pre>{@code
 * { "path": "<identity-path>" }
 * }</pre>
 *
 * <p>Responses:</p>
 * <ul>
 *   <li><b>200</b> with {@code List<VersionMeta>} (empty if the path has no versions or does not exist).</li>
 * </ul>
 */
	@PostMapping(path="/api/read/all/versions", 
			consumes = MediaType.APPLICATION_JSON_VALUE, 
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<VersionMeta>> getAllVersion(@RequestBody Map<String, String> request) {
		try {
		String path = request.get("path");
		Objects.requireNonNull(path, "path");
		
		String storePath = PathUtils.normalizeAndApplyNamespace(path, namespace);
		 
		List<VersionMeta> vms = storeService.getAllVersions(storePath);
		List<VersionMeta> clientVms = APIUtils.toClientVersionMeta(vms, namespace);
		
		return ResponseEntity.ok(clientVms);
		
		} catch (Exception e) {
			LOGGER.error("getAllVersions failed", e);
			return ResponseEntity.internalServerError().build();
		}
	}
	
	
	/**
 * Convenience: read the raw content bytes of the latest (HEAD) version at a path.
 *
 * <p>Request body:</p>
 * <pre>{@code
 * { "path": "<identity-path>" }
 * }</pre>
 *
 * <p>Responses (Optional semantics):</p>
 * <ul>
 *   <li><b>200</b> with the blob bytes for the latest version.</li>
 *   <li><b>404</b> if the path does not exist, has no versions, or the underlying content hash is missing.</li>
 * </ul>
 */
	@PostMapping(path="/api/read/blob/content/latest/version",
			consumes = MediaType.APPLICATION_JSON_VALUE
			 )
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
		
		return ResponseEntity.notFound().build();
		
		} catch (Exception e) {
			LOGGER.error("getBlobLatestVersion failed", e);
			return ResponseEntity.internalServerError().build();
		}
	}
	
	/**
 * Read {@link PathMeta} for a path (children + version listing as configured by the store).
 *
 * <p>Request body:</p>
 * <pre>{@code
 * { "path": "<identity-path>" }
 * }</pre>
 *
 * <p>Responses (Optional semantics):</p>
 * <ul>
 *   <li><b>200</b> with {@link PathMeta} if present.</li>
 *   <li><b>404</b> if the path does not exist.</li>
 * </ul>
 */
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
			
			List<VersionMeta> fixedVersions = APIUtils.toClientVersionMeta(m.versions(), namespace);

			PathMeta mx = new PathMeta(fixedPath, fixedChildren, fixedVersions);

			return ResponseEntity.ok(mx);
		} catch (Exception e) {
			LOGGER.error("Unable to get meta for {}", path, e);
		}
		return ResponseEntity.internalServerError().build();
	}
	
	/**
	 * Single-operation multipart processor (legacy / convenience endpoint).
	 *
	 * <p>Consumes {@code multipart/form-data} with:</p>
	 * <ul>
	 *   <li>{@code intent}: JSON map {@code {"path":"...","operation":"..."}}</li>
	 *   <li>{@code blob} or {@code file}: optional uploaded content (depends on operation)</li>
	 * </ul>
	 *
	 * <p>Context headers (optional) are parsed via {@link ContextResolvers}:
	 * {@code X-Transaction-Id}, {@code X-Correlation-Id}, {@code X-Workflow-Id}, {@code X-Principal},
	 * {@code X-Context-Name}.</p>
	 *
	 * <p>Responses (Optional semantics):</p>
	 * <ul>
	 *   <li><b>200</b> with {@link VersionMeta} if the operation produced a version.</li>
	 *   <li><b>404</b> if no version was produced (e.g., target not found).</li>
	 * </ul>
	 */
	@PostMapping(path = "/api/processor", 
			consumes = {MediaType.MULTIPART_FORM_DATA_VALUE},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<VersionMeta> processMultipart(
			@RequestPart("intent") String intentJson,
			@RequestPart(name = "blob", required=false) MultipartFile blob,
			@RequestPart(name = "file", required=false) MultipartFile file,
			MultipartHttpServletRequest request) throws Exception 
	
	 {
		
		Map<String, String> action = JSON.readValue(intentJson, new TypeReference<Map<String, String>>() {
		});
			 
			String path = action.get("path");
			String operation = action.get("operation");
			
			if (file == null) {
				file = blob;
			}
			
			Objects.requireNonNull(path);
			Objects.requireNonNull(operation);

			path = PathUtils.normalizeAndApplyNamespace(path, namespace);
			
			final InputStream is;
			String name = "blob";
			String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
		
			if (file != null) {
				is = file.getInputStream();
				name = file.getOriginalFilename() != null ?  file.getOriginalFilename() : name; 
				mimeType = StringUtils.isEmpty(file.getContentType()) ? mimeType : file.getContentType();
			} else {
				is = null;
			}

			
			//get context from headers if any
			//if transactionId was not provided use the provided UUID
			ContextSnapshot ctx = ContextResolvers.fromHttpHeaders(
					request.getRequestHeaders().toSingleValueMap(),
					null
			);
			
			try (is; @SuppressWarnings("unused")
			ContextScope scope = Context.scope(ctx)) {
				Optional<VersionMeta> vm = storeService.process(operation, path, new BlobMeta(name, mimeType) , is);
				if (vm.isPresent())
					return ResponseEntity.ok(APIUtils.toClientVersionMeta(vm.get(), namespace));
			}	
		    
		
		return ResponseEntity.notFound().build();
	}

	/**
 * Transaction batch processor (demo authority).
 *
 * <p>Consumes {@code multipart/form-data} with:</p>
 * <ul>
 *   <li>{@code transactions}: JSON array of {@link Transaction}</li>
 *   <li>zero or more file parts, where the part name equals {@link Transaction#blobRef()}</li>
 * </ul>
 *
 * <p>Contract:</p>
 * <ul>
 *   <li>If a transaction references {@code blobRef}, the multipart request must include a part with that exact name,
 *       otherwise <b>400</b> is returned.</li>
 *   <li>All context headers are optional and parsed via {@link ContextResolvers}
 *       ({@code X-Transaction-Id}, {@code X-Correlation-Id}, {@code X-Workflow-Id}, {@code X-Principal}).</li>
 * </ul>
 *
 * <p>Response is a small JSON summary (debug-friendly) rather than a domain DTO.</p>
 *
 * @throws Exception on processing failures (mapped by Spring exception handling)
 */
	
	@PostMapping(path = "/api/txn/processor", 
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE, 
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> processTransactionMultipart(@RequestPart("transactions") String transactionsJson,
			MultipartHttpServletRequest request) throws Exception {

		List<Transaction> txns = JSON.readValue(transactionsJson, new TypeReference<List<Transaction>>() {
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
		Map<String, InputStream> blobs = new LinkedHashMap<>();
		Map<String, BlobMeta> metas = new LinkedHashMap<>();
		
		for (var e : fileMap.entrySet()) {
			String blobRef = e.getKey();
			MultipartFile f = e.getValue();
			
			blobs.put(blobRef, f.getInputStream());
			
			String ct = f.getContentType();
			if (StringUtils.isEmpty(ct))   ct = MediaType.APPLICATION_OCTET_STREAM_VALUE;
			
			String name = f.getOriginalFilename();
			if (StringUtils.isEmpty(name)) name = blobRef;
			
			metas.put(blobRef, new BlobMeta(name, ct));
		}
		
		//apply namespace to paths
		List<Transaction> namespacetxns = new ArrayList<>();
		for (Transaction t : txns) {
			namespacetxns.add(new Transaction(
				PathUtils.normalizeAndApplyNamespace(t.path(), namespace),
				t.blobRef(),
				t.blobMimetype(),
				t.operation()
			));
		}
		
		APITransaction apiTxn = new APITransaction(namespacetxns,  blobs);

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
		
			List<VersionMeta> before = storeService.processTransaction(apiTxn, metas);
			
			summary.put("before", APIUtils.toClientVersionMeta(before, namespace));
			
			List<VersionMeta> after = new ArrayList<>();
			
			for (Transaction t : apiTxn.transactions()) {
				Optional<VersionMeta> opt = storeService.getLatestVersion(t.path());
				if (opt.isPresent()) {
					after.add(APIUtils.toClientVersionMeta(opt.get(), namespace));
				}	
			}
			
			summary.put("after", after);
			
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
