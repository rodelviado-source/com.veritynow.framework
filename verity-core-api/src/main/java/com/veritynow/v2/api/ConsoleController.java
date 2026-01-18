package com.veritynow.v2.api;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.veritynow.v2.store.core.PathEvent;
import com.veritynow.v2.store.meta.PathMeta;
import com.veritynow.v2.store.meta.VersionMeta;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class ConsoleController {

	private final ConsoleService consoleService;
	private static final Logger LOGGER = LogManager.getLogger();

	private final String namespace;

	public ConsoleController(ConsoleService consoleService, @Value("${verity.api.namespace:/vn}") String namespace) {
		this.consoleService = consoleService;
		this.namespace = NamespaceUtils.normalizeNamespace(namespace);
	}

	/**
	 * IMPORTANT: We purposely do NOT use @RequestMapping("/api") here. We match the
	 * same /api/** space BUT ONLY when params include meta=true.
	 *
	 * This prevents collision with APIController's normal GET /api/**.
	 */
	@GetMapping(value = "/api/**", params = "meta=true", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PathMeta> meta(HttpServletRequest req) {
		
		try {
		
		String merklePath = NamespaceUtils.applyNamespace(req, namespace);

		Optional<PathMeta> opt = consoleService.getPathMeta(merklePath);
		if (opt.isEmpty())
			return ResponseEntity.notFound().build();

		// Strip namespace for EVERYTHING returned to the client
		
		PathMeta m = opt.get();
		String fixedPath = NamespaceUtils.removeNamespace(m.path(), namespace);
		List<String> fixedChildren = m.children();
		
		if (fixedChildren != null) {
			fixedChildren = m.children().stream().map((s) -> NamespaceUtils.lastSegment(s)).toList();
		}
		
		List<VersionMeta> fixedVersions = m.versions().stream().
				map((v) -> 
				{
					 String vmPath = NamespaceUtils.removeNamespace(v.path(), namespace);
					 return new VersionMeta(v.blobMeta(), new PathEvent(vmPath, v.timestamp(),  v.operation(), v.principal(), v.correlationId(), v.workflowId(), v.contextName(), v.transactionId(), v.transactionResult()));
				     
				}
		).toList();
		
		PathMeta mx = new PathMeta(
		   fixedPath, 
		   fixedChildren, fixedVersions );
		
		return ResponseEntity.ok(mx);
		} catch (Exception e) {
			LOGGER.error("Unable to get meta for {}", req.getRequestURI(), e);
		}
		return ResponseEntity.badRequest().build();
	}

	@GetMapping(value = "/api/**", params = "versions=true", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<VersionMeta>> versions(HttpServletRequest req) {
		
		String merklePath = NamespaceUtils.applyNamespace(req, namespace);

		Optional<List<VersionMeta>> opt = consoleService.listAllVersions(merklePath);
		
		if (opt.isPresent()) {
			return ResponseEntity.ok(opt.get());
		}
		
		return ResponseEntity.ok(List.of());

	}

	/**
	 * GET /api/_content/{hash}
	 *
	 * This is what RemoteTransport.getBytesByHash() calls. Embedded never uses this
	 * endpoint (it reads OPFS directly).
	 */
	@GetMapping(value = "/api/_content/{hash}")
	public ResponseEntity<InputStreamResource> loadBytesByHash(HttpServletRequest req, @PathVariable("hash") String hash) {
		

		Optional<InputStream> opt = consoleService.loadBytesByHash(hash);
		if (opt.isEmpty())
			return ResponseEntity.notFound().build();

		HttpHeaders h = new HttpHeaders();
		h.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		return ResponseEntity.ok().headers(h).body(new InputStreamResource(opt.get()));
	}

	
	
	

	

	
	
}
