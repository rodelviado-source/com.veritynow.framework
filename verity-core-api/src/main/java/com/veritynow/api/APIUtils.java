package com.veritynow.api;

import java.net.URI;

import com.veritynow.core.store.meta.VersionMeta;
import com.veritynow.core.store.versionstore.PathUtils;

import jakarta.servlet.http.HttpServletRequest;

public class APIUtils {

	/*
	 * ========================================================= HTTP → internal
	 * (Ingress) =========================================================
	 */

	
	public static String applyNamespace(HttpServletRequest request, String namespace) {
		// 1) Decode HTTP path correctly (percent-decoding, '+' preserved)
		String decodedPath = decodePathFromHttpRequest(request);

		// 2) Apply namespace (internal prefix)
		return PathUtils.normalizeAndApplyNamespace(decodedPath, namespace);
	}

	public static String decodePathFromHttpRequest(HttpServletRequest request) {
		URI uri = URI.create(request.getRequestURI());
		return uri.getPath(); // decoded, no query, no fragment
	}


	

	/**
	 * Convert a Merkle-level BlobMeta (whose path includes the namespace) back into
	 * a client-visible BlobMeta whose path is the public API path.
	 *
	 * Example: namespace = "/vn" meta.path = "/vn/api/agents/a1/clients/c9" →
	 * clientMeta.path = "/api/agents/a1/clients/c9"
	 */
	public static VersionMeta toClientVersionMeta(VersionMeta vm, String namespace) {
		String path = vm.path();
		String clientPath = PathUtils.removeNamespace(path, namespace);
		return new VersionMeta(
				vm.hashAlgorithm(),
				vm.hash(), vm.name(), vm.mimeType(), vm.size(),

				clientPath, vm.timestamp(), vm.operation(), vm.principal(), vm.correlationId(), vm.workflowId(), vm.contextName(), vm.transactionId(), vm.transactionResult());
	}

}
