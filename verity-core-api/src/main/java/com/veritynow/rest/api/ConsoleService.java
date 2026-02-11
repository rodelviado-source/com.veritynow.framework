package com.veritynow.rest.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.veritynow.core.context.ContextScope;
import com.veritynow.core.store.TransactionAndLockingAware;
import com.veritynow.core.store.base.PK;
import com.veritynow.core.store.meta.BlobMeta;
import com.veritynow.core.store.meta.VersionMeta;
import com.veritynow.core.store.versionstore.CloseableLockHandle;
import com.veritynow.core.store.versionstore.repo.RepositoryManager;
import com.veritynow.core.store.versionstore.repo.VersionMetaRepository;

@Service
public class ConsoleService {

	private final TransactionAndLockingAware<PK, BlobMeta, VersionMeta, ContextScope, CloseableLockHandle> versionStore;
	private final VersionMetaRepository versionMetaRepository;
	private final RepositoryManager repoManager;
	private static final Logger LOGGER = LogManager.getLogger();

	public ConsoleService(
			TransactionAndLockingAware<PK, BlobMeta, VersionMeta, ContextScope, CloseableLockHandle> versionStore,
			VersionMetaRepository versionMetaRepository, RepositoryManager repoManager, APIURLMappedPathNotGoodExampleService apiService) {
		this.versionStore = versionStore;
		this.versionMetaRepository = versionMetaRepository;
		this.repoManager = repoManager;
		LOGGER.info("Console Service using " + versionStore.getClass().getName());
	}

	public List<VersionMeta> getVersionsByTransactionId(String transactionId) {
		Objects.requireNonNull(transactionId, "transactionId");
		return versionMetaRepository.findByTransactionId(transactionId);
	}

	public List<VersionMeta> getVersionsByCorrelationId(String correlationId) {
		Objects.requireNonNull(correlationId, "correlationId");
		return versionMetaRepository.findByCorrelationId(correlationId);
	}

	public List<VersionMeta> getVersionsByCorrelationIdAndTransaction(String correlationId, String transactionId) {
		Objects.requireNonNull(correlationId, "correlationId");
		Objects.requireNonNull(transactionId, "transactionId");
		return versionMetaRepository.findByCorrelationIdAndTransactionId(correlationId, transactionId);
	}

	public List<VersionMeta> getVersionsByWorkflowId(String workflowId) {
		Objects.requireNonNull(workflowId, "workflowId");
		return versionMetaRepository.findByWorkflowId(workflowId);
	}

	public List<VersionMeta> getVersionsByWorkflowIdAndCorrelationId(String workflowId, String correlationId) {
		Objects.requireNonNull(workflowId, "workflowId");
		Objects.requireNonNull(correlationId, "correlationId");
		return versionMetaRepository.findByWorkflowIdAndCorrelationId(workflowId, correlationId);
	}

	public List<VersionMeta> getVersionsByWorkflowIdAndCorrelationIdAndTransationId(String workflowId,
			String correlationId, String transactionId) {
		Objects.requireNonNull(workflowId, "workflowId");
		Objects.requireNonNull(correlationId, "correlationId");
		Objects.requireNonNull(correlationId, "transactionId");
		return versionMetaRepository.findByWorkflowIdAndCorrelationIdAndTransactionId(workflowId, correlationId,
				transactionId);
	}

	public List<VersionMeta> getWorkflows(String path) {
		Objects.requireNonNull(path, "path");
		return repoManager.getWorkflows(path);
	}


	public List<VersionMeta> getAllVersions(String path) throws IOException {
		Objects.requireNonNull(path, "path");
		return versionStore.getAllVersions(path);
	}

	public Optional<InputStream> getContent(String hash) throws IOException {
		Optional<InputStream> opt = versionStore.getContent(PK.hash(hash));
		if (opt.isPresent())
			return opt;
		return Optional.empty();
	}

	public Optional<VersionMeta> undelete(String path) {
		try {
			Optional<BlobMeta> opt = versionStore.undelete(PK.path(path));
			if (opt.isPresent())
				return versionStore.getLatestVersion(path);
		} catch (IOException e) {
			LOGGER.error("Unable to delete {}", path, e);
		}
		return Optional.empty();
	}



	

}
