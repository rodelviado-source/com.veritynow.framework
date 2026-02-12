package com.veritynow.core.store.versionstore.repo;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.veritynow.core.store.meta.VersionMeta;
import com.veritynow.core.store.versionstore.PathUtils;
import com.veritynow.core.store.versionstore.model.DirEntry;
import com.veritynow.core.store.versionstore.model.Inode;

public class RepositoryManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private final InodeRepository inodeRepo;
    private final VersionMetaRepository verRepo;

    public RepositoryManager(InodeRepository inodeRepo, VersionMetaRepository verRepo) {
        Objects.requireNonNull(inodeRepo, "Inode Repository required");
        Objects.requireNonNull(verRepo, "Version Repository required");
        this.inodeRepo = inodeRepo;
        this.verRepo = verRepo;

        LOGGER.info("Inode backed Repository Manger started");
    }

    public List<VersionMeta> getWorkflows(String path) {
        Objects.requireNonNull(path, "nodePath");
        path = PathUtils.normalizePath(path);
        Optional<Long> inodeId = inodeRepo.resolveInodeId(path);
        if (inodeId.isPresent())
            return verRepo.getWorkflows(inodeId.get());
        return List.of();
    }

    public List<VersionMeta> getCorrelations(String nodePath) {
        Objects.requireNonNull(nodePath, "nodePath");
        nodePath = PathUtils.normalizePath(nodePath);
        Optional<Long> inodeId = inodeRepo.resolveInodeId(nodePath);
        if (inodeId.isPresent())
            return verRepo.getCorrelations(inodeId.get());
        return List.of();
    }

    public List<VersionMeta> getTransactions(String nodePath) {
        Objects.requireNonNull(nodePath, "nodePath");
        nodePath = PathUtils.normalizePath(nodePath);
        Optional<Long> inodeId = inodeRepo.resolveInodeId(nodePath);
        if (inodeId.isPresent())
            return verRepo.getTransactions(inodeId.get());
        return List.of();
    }

    public Optional<VersionMeta> getLatestVersion(String nodePath) throws IOException {
        Objects.requireNonNull(nodePath, "nodePath");
        nodePath = PathUtils.normalizePath(nodePath);
        Optional<Long> inodeIdOpt = inodeRepo.resolveInodeId(nodePath);
        if (inodeIdOpt.isEmpty()) return Optional.empty();

        Long inodeId = inodeIdOpt.get();
        return verRepo.findLatestVersionByInodeId(inodeId, nodePath);
    }

    public boolean pathExists(String path) {
        Objects.requireNonNull(path, "path");
        return inodeRepo.pathExists(path);
    }

    /**
     * N+1 fix:
     *  - Query children once
     *  - Query heads for all child inode IDs once
     */
    public List<VersionMeta> getChildrenLatestVersion(String nodePath) throws IOException {
        Objects.requireNonNull(nodePath, "nodePath");

        nodePath = PathUtils.normalizePath(nodePath);

        Optional<Long> inodeIdOpt = inodeRepo.resolveInodeId(nodePath);
        if (inodeIdOpt.isEmpty()) return List.of();

        Long inodeId = inodeIdOpt.get();

        List<DirEntry> children = inodeRepo.findAllByParentIdOrderByNameAsc(inodeId);
        if (children.isEmpty()) return List.of();
        

        return verRepo.findLatestVersionsForDirectChildren(nodePath, children);
    }

    public List<String> getChildrenPath(String nodePath) throws IOException {
        Objects.requireNonNull(nodePath, "nodePath");
        nodePath = PathUtils.normalizePath(nodePath);

        Optional<Long> inodeIdOpt = inodeRepo.resolveInodeId(nodePath);
        if (inodeIdOpt.isEmpty()) return List.of();

        Long inodeId = inodeIdOpt.get();
        String np = PathUtils.trimEndingSlash(nodePath);

        List<DirEntry> children = inodeRepo.findAllByParentIdOrderByNameAsc(inodeId);
        return children.stream()
                .map(de -> np + "/" + de.name())
                .collect(Collectors.toList());
    }

    public List<VersionMeta> getAllVersions(String nodePath) throws IOException {
        Objects.requireNonNull(nodePath, "nodePath");
        nodePath = PathUtils.normalizePath(nodePath);
        Optional<Long> inodeIdOpt = inodeRepo.resolveInodeId(nodePath);
        if (inodeIdOpt.isEmpty()) return List.of();
        Long inodeId = inodeIdOpt.get();
        return verRepo.findAllByInodeIdWithPath(inodeId, nodePath);
    }

    public VersionMeta persist(VersionMeta vm) {
        Inode inode = inodeRepo.resolveOrCreateInode(vm.path());
        return verRepo.persist(vm, inode.id());
    }

    public VersionMeta persistAndPublish(VersionMeta vm) {
        Inode inode = inodeRepo.resolveOrCreateInode(vm.path());
        return verRepo.persistAndPublish(vm, inode.id());
    }
    
    public void ensureRootInode() {
        inodeRepo.ensureRootInode();
    }

}
