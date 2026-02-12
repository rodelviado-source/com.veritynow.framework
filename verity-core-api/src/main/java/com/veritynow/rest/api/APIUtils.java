package com.veritynow.rest.api;

import java.util.List;

import com.veritynow.core.store.meta.VersionMeta;
import com.veritynow.core.store.versionstore.PathUtils;

public class APIUtils {

	/*
	 * ========================================================= HTTP → internal
	 * (Ingress) =========================================================
	 */

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

	
	public static List<VersionMeta> toClientVersionMeta(List<VersionMeta> l, String namespace) {
		return l.stream().map(
				(vm) -> {return APIUtils.toClientVersionMeta(vm, namespace);}
			).toList();
	}
}
