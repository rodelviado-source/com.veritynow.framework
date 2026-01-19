package com.veritynow.api;

import java.net.URI;

import com.veritynow.core.store.meta.VersionMeta;

import jakarta.servlet.http.HttpServletRequest;

public class NamespaceUtils {

	/*
	 * ========================================================= HTTP → internal
	 * (Ingress) =========================================================
	 */

	/**
	 * Convert an HTTP request URI into a decoded, canonical internal path.
	 *
	 * Rules: - Percent-decoding via URI.getPath() - '+' is preserved as '+' - Query
	 * and fragment are ignored - Result is a decoded, absolute internal path -
	 * Optional namespace is applied
	 */

	public static String applyNamespace(String path, String namespace) {
		String ns = normalizeNamespace(namespace);
		String p = normalizeAbsolutePath(path);

		if (ns.isEmpty())
			return p;
		if (p.equals("/"))
			return ns;
		return ns + p;
	}

	public static String applyNamespace(HttpServletRequest request, String namespace) {
		// 1) Decode HTTP path correctly (percent-decoding, '+' preserved)
		String decodedPath = decodePathFromHttpRequest(request);

		// 2) Apply namespace (internal prefix)
		return applyNamespace(decodedPath, namespace);
	}

	public static String decodePathFromHttpRequest(HttpServletRequest request) {
		URI uri = URI.create(request.getRequestURI());
		return uri.getPath(); // decoded, no query, no fragment
	}

	/*
	 * ========================================================= internal →
	 * Client/Public (Egress)
	 * =========================================================
	 */

	/**
	 * Strip namespace from an internal internal path so it is not leaked to clients
	 * or UI.
	 */
	public static String removeNamespace(String internalPath, String namespace) {
		String ns = normalizeNamespace(namespace);
		String p = normalizeAbsolutePath(internalPath);

		if (ns.isEmpty())
			return p;
		if (p.equals(ns))
			return "/";
		if (p.startsWith(ns + "/"))
			return p.substring(ns.length());
		return p;
	}

	/*
	 * ========================================================= internal helpers
	 * (decoded paths only)
	 * =========================================================
	 */

	/**
	 * Normalize namespace: - null / blank / "/" → "" - otherwise → "/x/y" (no
	 * trailing slash)
	 */
	public static String normalizeNamespace(String namespace) {
		if (namespace == null)
			return "";

		String ns = namespace.trim();
		if (ns.isEmpty() || ns.equals("/"))
			return "";

		// remove leading slashes
		ns = ns.replaceAll("^/+", "");
		// remove trailing slashes
		ns = ns.replaceAll("/+$", "");

		return ns.isEmpty() ? "" : ("/" + ns);
	}

	/**
	 * Normalize an absolute internal path: - always starts with "/" - collapses
	 * "//" - no trailing "/" except root
	 */
	public static String normalizeAbsolutePath(String path) {
		if (path == null || path.isBlank())
			return "/";

		String p = path.trim();

		if (!p.startsWith("/"))
			p = "/" + p;

		// collapse repeated slashes
		p = p.replaceAll("/{2,}", "/");

		// remove trailing slash except root
		if (p.length() > 1)
			p = p.replaceAll("/+$", "");

		return p;
	}
	
	public static String lastSegment(String path) {
        int i = path.lastIndexOf('/');
        return (i >= 0) ? path.substring(i + 1) : path;
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
		String clientPath = NamespaceUtils.removeNamespace(path, namespace);
		return new VersionMeta(

				vm.hash(), vm.name(), vm.mimeType(), vm.size(),

				clientPath, vm.timestamp(), vm.operation(), vm.principal(), vm.correlationId(), vm.workflowId(), vm.contextName(), vm.transactionId(), vm.transactionResult());
	}

}
