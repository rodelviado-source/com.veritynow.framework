package com.veritynow.rest.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.veritynow.core.store.meta.VersionMeta;
import com.veritynow.core.store.versionstore.PathEvent;
import com.veritynow.core.store.versionstore.PathUtils;

@RestController
public class ConsoleController {

	private final ConsoleService consoleService;
	private static final Logger LOGGER = LogManager.getLogger();

	private final String namespace;

	public ConsoleController(ConsoleService consoleService, @Value("${verity.api.namespace:/vn}") String namespace) {
		this.consoleService = consoleService;
		this.namespace = PathUtils.normalizeNamespace(namespace);
	}

	@GetMapping("/api/audit/workflow/{workflowId}")
	public List<VersionMeta> getVersionsByWorkflow(@PathVariable String workflowId) {
		try {
			List<VersionMeta> vs = consoleService.getVersionsByWorkflowId(workflowId);
			return vs.stream().map((v) -> {
				String vmPath = PathUtils.removeNamespace(v.path(), namespace);
				return new VersionMeta(v.blobMeta(), new PathEvent(vmPath, v.timestamp(), v.operation(), v.principal(),
						v.correlationId(), v.workflowId(), v.contextName(), v.transactionId(), v.transactionResult()));

			}).toList();
		} catch (Exception e) {
			LOGGER.error("Unable to get versions for workflow = {}", workflowId, e);
		}
		return List.of();
	}
	
	@PostMapping(path ="/api/audit/workflows", 
			produces = MediaType.APPLICATION_JSON_VALUE)
	public List<VersionMeta> getWorkflows(
			@RequestBody Map<String, String> request
			) {
		
		String path = request.get("path");
		Objects.requireNonNull(path, "path");
		try {
			path =  PathUtils.normalizeAndApplyNamespace(path, namespace);
			
			List<VersionMeta> vs = consoleService.getWorkflows(path);
			return vs.stream().map((v) -> {
				String vmPath = PathUtils.removeNamespace(v.path(), namespace);
				return new VersionMeta(v.blobMeta(), new PathEvent(vmPath, v.timestamp(), v.operation(), v.principal(),
						v.correlationId(), v.workflowId(), v.contextName(), v.transactionId(), v.transactionResult()));

			}).toList();
		} catch (Exception e) {
			LOGGER.error("Unable to get workflows for path = {}", path, e);
		}
		return List.of();

	}

	@GetMapping("/api/audit/workflow/{workflowId}/corr/{correlationId}")
	public List<VersionMeta> getVersionsByWorkflowAndCorrelation(@PathVariable String workflowId,
			@PathVariable String correlationId) {
		try {
			List<VersionMeta> vs = consoleService.getVersionsByWorkflowIdAndCorrelationId(workflowId, correlationId);
			return vs.stream().map((v) -> {
				String vmPath = PathUtils.removeNamespace(v.path(), namespace);
				return new VersionMeta(v.blobMeta(), new PathEvent(vmPath, v.timestamp(), v.operation(), v.principal(),
						v.correlationId(), v.workflowId(), v.contextName(), v.transactionId(), v.transactionResult()));

			}).toList();
		} catch (Exception e) {
			LOGGER.error("Unable to get versions for workflow/correlation = {}/{}", workflowId, correlationId, e);
		}
		return List.of();
	}

	@GetMapping("/api/audit/workflow/{workflowId}/corr/{correlationId}/txn/{transactionId}")
	public List<VersionMeta> getVersionsByWorkflowAndCorrelationAndTransaction(@PathVariable String workflowId,
			@PathVariable String correlationId, @PathVariable String transactionId) {
		try {
			List<VersionMeta> vs = consoleService.getVersionsByWorkflowIdAndCorrelationIdAndTransationId(workflowId,
					correlationId, transactionId);
			return vs.stream().map((v) -> {
				String vmPath = PathUtils.removeNamespace(v.path(), namespace);
				return new VersionMeta(v.blobMeta(), new PathEvent(vmPath, v.timestamp(), v.operation(), v.principal(),
						v.correlationId(), v.workflowId(), v.contextName(), v.transactionId(), v.transactionResult()));

			}).toList();
		} catch (Exception e) {
			LOGGER.error("Unable to get versions for workflow/correlation/tranasaction = {}/{}/{}", workflowId,
					correlationId, transactionId, e);
		}
		return List.of();
	}

	@GetMapping("/api/audit/corr/{correlationId}")
	public List<VersionMeta> getVersionsByCorrelation(@PathVariable String correlationId) {
		try {
			List<VersionMeta> vs = consoleService.getVersionsByCorrelationId(correlationId);
			return vs.stream().map((v) -> {
				String vmPath = PathUtils.removeNamespace(v.path(), namespace);
				return new VersionMeta(v.blobMeta(), new PathEvent(vmPath, v.timestamp(), v.operation(), v.principal(),
						v.correlationId(), v.workflowId(), v.contextName(), v.transactionId(), v.transactionResult()));

			}).toList();
		} catch (Exception e) {
			LOGGER.error("Unable to get versions for correlation= {}", correlationId, e);
		}
		return List.of();
	}

	@GetMapping("/api/audit/corr/{correlationId}/{transactionId}")
	public List<VersionMeta> getVersionsByCorrelationAndTransaction(@PathVariable String correlationId,
			@PathVariable String transactionId) {
		try {
			List<VersionMeta> vs = consoleService.getVersionsByCorrelationIdAndTransaction(correlationId,
					transactionId);
			return vs.stream().map((v) -> {
				String vmPath = PathUtils.removeNamespace(v.path(), namespace);
				return new VersionMeta(v.blobMeta(), new PathEvent(vmPath, v.timestamp(), v.operation(), v.principal(),
						v.correlationId(), v.workflowId(), v.contextName(), v.transactionId(), v.transactionResult()));

			}).toList();
		} catch (Exception e) {
			LOGGER.error("Unable to get versions for correlation/tranasaction = {}/{}", correlationId, transactionId,
					e);
		}
		return List.of();
	}

	@GetMapping("/api/audit/txn/{transactionId}")
	public List<VersionMeta> getVersionsByTransaction(@PathVariable String transactionId) {
		try {
			List<VersionMeta> vs = consoleService.getVersionsByTransactionId(transactionId);
			return vs.stream().map((v) -> {
				String vmPath = PathUtils.removeNamespace(v.path(), namespace);
				return new VersionMeta(v.blobMeta(), new PathEvent(vmPath, v.timestamp(), v.operation(), v.principal(),
						v.correlationId(), v.workflowId(), v.contextName(), v.transactionId(), v.transactionResult()));

			}).toList();
		} catch (Exception e) {
			LOGGER.error("Unable to get versions for tranasaction = {}", transactionId, e);
		}
		return List.of();
	}
	


	
}
