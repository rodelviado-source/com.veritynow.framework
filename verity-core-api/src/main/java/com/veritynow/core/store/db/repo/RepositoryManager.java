package com.veritynow.core.store.db.repo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.veritynow.core.store.db.PathUtils;
import com.veritynow.core.store.db.model.DirEntry;
import com.veritynow.core.store.db.model.Inode;
import com.veritynow.core.store.db.model.InodePathSegment;
import com.veritynow.core.store.meta.VersionMeta;



public class RepositoryManager {
	private static final Logger LOGGER = LogManager.getLogger();
	private final InodeRepository inodeRepo;
	private final VersionMetaRepository verRepo;

	public RepositoryManager(InodeRepository inodeRepo,  VersionMetaRepository verRepo) {
		Objects.requireNonNull(inodeRepo, "Inode Repository required");
		Objects.requireNonNull(verRepo,    "Version Repository required");
		this.inodeRepo = inodeRepo;
		this.verRepo = verRepo;
		
		LOGGER.info("Inode Manger started");
		
	}


	public Optional<VersionMeta> getLatestVersionInodeId(Long id) {
		return  verRepo.findLatestVersionByInodeId(id);
	}
	
	public List<DirEntry> findAllByParentId(Long id) { 
		return inodeRepo.findAllByParentIdOrderByNameAsc(id);
	}
	
	public List<VersionMeta> findAllByInodeIdOrderByTimestampDescIdDesc(Long inodeId) {
	 return verRepo.findAllByInodeIdOrderByTimestampDescIdDesc(inodeId);
	} 
	
	public VersionMeta saveVersionMeta(VersionMeta vm) {
		Inode inode = resolveOrCreateInode(vm.path());
		return verRepo.save(vm, inode.id());
	}
	
	public Inode rootInode() {
	  Long rootId = inodeRepo.findIdByScopeKey(PathKeyCodec.ROOT_LABEL)
	      .orElseThrow(() -> new IllegalStateException("Root inode missing for scope_key=" + PathKeyCodec.ROOT_LABEL));
	  return inodeRepo.findById(rootId)
	      .orElseThrow(() -> new IllegalStateException("Root inode missing id=" + rootId));
	}

	
	/**
	 * Resolve a normalized absolute path to its inode's precomputed scope_key.
	 *
	 * Store-only helper intended for consumers (e.g., locking) that must not
	 * re-implement hashing/ltree codecs.
	 */
	public Optional<String> resolveScopeKey(String nodePath) {
		Objects.requireNonNull(nodePath, "nodePath");
		String scopeKey = PathKeyCodec.toLTree(nodePath);
		return inodeRepo.findIdByScopeKey(scopeKey).isPresent() ? Optional.of(scopeKey) : Optional.empty();
	}
    
	public Optional<String> resolvePathFromInode(Long inodeId) {
        Objects.requireNonNull(inodeId, "inodeId");

		Optional<Inode> inodeOpt = inodeRepo.findById(inodeId);
		if (inodeOpt.isEmpty()) return Optional.empty();
		
		Inode inode = inodeOpt.get();
		Inode root = rootInode();
		
		if (inode.id().equals(root.id())) return Optional.of("/");
		
		List<InodePathSegment> segs = inodeRepo.findAllByInodeIdOrderByOrdAsc(inode.id());
		if (segs.isEmpty()) {
			// No backfill/repair in Phase-1: projection must exist for this inode.
			LOGGER.warn("Backfill needed");
			return Optional.empty();
		}
		
		List<String> names = new ArrayList<>(segs.size());
		for (InodePathSegment s : segs) {
			names.add(s.dirEntry().name());
		}
		return Optional.of("/" + String.join("/", names));
    }

    // -----------------------------
    // inode path resolution / creation (no full-path column)
    // -----------------------------

    public Inode resolveOrCreateInode(String nodePath) {
        List<String> segs = PathUtils.splitSegments(nodePath);
        Inode cur = rootInode();

        for (String seg : segs) {
            Optional<DirEntry> e = inodeRepo.findByParentIdAndName(cur.id(), seg);
            if (e.isPresent()) {
                cur = e.get().child();
                continue;
            }
            String childScopeKey = PathKeyCodec.appendSegLabel(cur.scopeKey(), PathKeyCodec.label(seg));
            
            Inode child = inodeRepo.save(new Inode(Instant.now(), childScopeKey));
			DirEntry entry = inodeRepo.save(new DirEntry(cur, seg, child));
			
			// Store-owned projection (Phase-1): inode -> ordered direntry chain
			List<InodePathSegment> parentSegs = inodeRepo.findAllByInodeIdOrderByOrdAsc(cur.id());
			List<InodePathSegment> childSegs = new ArrayList<>(parentSegs.size() + 1);
			for (InodePathSegment ps : parentSegs) {
				childSegs.add(new InodePathSegment(child, ps.ord(), ps.dirEntry()));
			}
			childSegs.add(new InodePathSegment(child, parentSegs.size(), entry));
			inodeRepo.saveAll(childSegs);
			
            cur = child;
        }
        return cur;
    }

    public void ensureRootInode() {
        // Store-owned bootstrap: root inode is the unique inode with scope_key = PathKeyCodec.ROOT_LABEL.
        // Also ensure the store-level invariant index exists (equality lookup by scope_key).
        inodeRepo.ensureScopeKeyUniqueIndex();
        if (inodeRepo.findIdByScopeKey(PathKeyCodec.ROOT_LABEL).isPresent()) {
            return;
        }
        inodeRepo.save(new Inode(Instant.now(), PathKeyCodec.ROOT_LABEL));
    }


	public Optional<Long> resolveInodeId(String nodePath) {
		return inodeRepo.resolveInodeId(nodePath);
	}

}
