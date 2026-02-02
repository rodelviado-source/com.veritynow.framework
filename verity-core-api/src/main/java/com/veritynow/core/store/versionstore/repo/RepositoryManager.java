package com.veritynow.core.store.versionstore.repo;

import java.io.IOException;
import java.util.ArrayList;
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
	

	public RepositoryManager(InodeRepository inodeRepo,  VersionMetaRepository verRepo) {
		Objects.requireNonNull(inodeRepo, "Inode Repository required");
		Objects.requireNonNull(verRepo,    "Version Repository required");
		this.inodeRepo = inodeRepo;
		this.verRepo = verRepo;
		
		LOGGER.info("Inode backed Repository Manger started");
		
	}
	
	

	public List<VersionMeta> getWorkflows(String nodePath) {
		Objects.requireNonNull(nodePath, "nodePath");
        nodePath = PathUtils.normalizePath(nodePath);
        Optional<Long> inodeId = inodeRepo.resolveInodeId(nodePath);
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
        return verRepo.findLatestVersionByInodeId(inodeId);
    }
	
	
	public List<VersionMeta> getChildrenLatestVersion(String nodePath) throws IOException {
        Objects.requireNonNull(nodePath, "nodePath");

        nodePath = PathUtils.normalizePath(nodePath);

        Optional<Long> inodeIdOpt = inodeRepo.resolveInodeId(nodePath);
        if (inodeIdOpt.isEmpty()) return List.of();

        Long inodeId = inodeIdOpt.get();
        List<VersionMeta> out = new ArrayList<>();
        Optional<VersionMeta> vmOpt;
        List<DirEntry> children = inodeRepo.findAllByParentIdOrderByNameAsc(inodeId);
        for (DirEntry child : children) {
            Long childId = child.child().id();
            vmOpt = verRepo.findLatestVersionByInodeId(childId);
            if (vmOpt.isPresent()) out.add(vmOpt.get());
		}

        return out;
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
	        return verRepo.findAllByInodeIdOrderByTimestampDescIdDesc(inodeId);
    }
	
	public VersionMeta persist(VersionMeta vm) {
		Inode inode = inodeRepo.resolveOrCreateInode(vm.path());
		return verRepo.save(vm, inode.id());
	}

    public void ensureRootInode() {
    	inodeRepo.ensureRootInode();
    }
    
    public void persistAndPublish(VersionMeta vm) {
    	Inode inode	 = inodeRepo.resolveOrCreateInode(vm.path());
    	verRepo.saveAndPublishMeta(vm, inode.id());
    }

// -----------------------------
// inode path resolution / creation (no full-path column)
// -----------------------------
	
//	private Optional<String> resolvePathFromInode(Long inodeId) {
//  Objects.requireNonNull(inodeId, "inodeId");
//
//	Optional<Inode> inodeOpt = inodeRepo.findById(inodeId);
//	if (inodeOpt.isEmpty()) return Optional.empty();
//	
//	Inode inode = inodeOpt.get();
//	Inode root = rootInode();
//	
//	if (inode.id().equals(root.id())) return Optional.of("/");
//	
//	List<InodePathSegment> segs = inodeRepo.findAllByInodeIdOrderByOrdAsc(inode.id());
//	if (segs.isEmpty()) {
//		// No backfill/repair in Phase-1: projection must exist for this inode.
//		LOGGER.warn("Backfill needed");
//		return Optional.empty();
//	}
//	
//	List<String> names = new ArrayList<>(segs.size());
//	for (InodePathSegment s : segs) {
//		names.add(s.dirEntry().name());
//	}
//	return Optional.of("/" + String.join("/", names));
//}
    
//    /**
//	 * Resolve a normalized absolute path to its inode's precomputed scope_key.
//	 *
//	 * Store-only helper intended for consumers (e.g., locking) that must not
//	 * re-implement hashing/ltree codecs.
//	 */
//	private Optional<String> resolveScopeKey(String nodePath) {
//		Objects.requireNonNull(nodePath, "nodePath");
//		String scopeKey = PathKeyCodec.toLTree(nodePath);
//		return inodeRepo.findIdByScopeKey(scopeKey).isPresent() ? Optional.of(scopeKey) : Optional.empty();
//	}

}
