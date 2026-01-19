package com.veritynow.v2.store.core.jpa;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class InodeManager {
	
	private final JdbcTemplate jdbc;
	private final InodeRepository inodeRepo;
	private final DirEntryRepository dirRepo;
	private final InodePathSegmentRepository pathSegRepo;
	private final VersionMetaHeadRepository headRepo;
	private final VersionMetaRepository verRepo;

	public InodeManager(JdbcTemplate jdbc, InodeRepository inodeRepo, DirEntryRepository dirRepo, InodePathSegmentRepository pathSegRepo, VersionMetaHeadRepository headRepo, VersionMetaRepository verRepo) {
		Objects.requireNonNull(jdbc, "JDBC required");
		Objects.requireNonNull(inodeRepo, InodeEntity.class.getName() + " Repository required");
		Objects.requireNonNull(dirRepo,   DirEntryEntity.class.getName() + " Repository required");
		Objects.requireNonNull(pathSegRepo,   InodePathSegmentEntity.class.getName() + " Repository required");
		Objects.requireNonNull(headRepo,  VersionMetaHeadEntity.class.getName() + " Repository required");
		Objects.requireNonNull(verRepo,   VersionMetaEntity.class.getName() + " Repository required");
		
		this.jdbc = jdbc;
		this.inodeRepo = inodeRepo;
		this.dirRepo = dirRepo;
		this.pathSegRepo = pathSegRepo;
		this.headRepo = headRepo;
		this.verRepo = verRepo;
	}


	public Optional<VersionMetaHeadEntity> getHeadById(Long id) {
		return  headRepo.findById(id);
	}
	
	public List<DirEntryEntity> findAllByParentId(Long id) { 
		return dirRepo.findAllByParent_Id(id);
	}
	
	public List<VersionMetaEntity> findAllByInodeIdOrderByTimestampDescIdDesc(Long inodeId) {
	 return verRepo.findAllByInode_IdOrderByTimestampDescIdDesc(inodeId);
	} 
	
	public VersionMetaEntity saveVersionMetaEntity(VersionMetaEntity vme) {
		return verRepo.save(vme);
	}
	
	public InodeEntity rootInode() {
	  Long rootId = inodeRepo.findIdByScopeKey(PathKeyCodec.ROOT_LABEL)
	      .orElseThrow(() -> new IllegalStateException("Root inode missing for scope_key=" + PathKeyCodec.ROOT_LABEL));
	  return inodeRepo.findById(rootId)
	      .orElseThrow(() -> new IllegalStateException("Root inode missing id=" + rootId));
	}

	public Optional<Long> resolveInodeId(String nodePath) {
		Objects.requireNonNull(nodePath, "nodePath");
		String scopeKey = scopeKeyForPath(nodePath);
		return inodeRepo.findIdByScopeKey(scopeKey);
    }

	/**
	 * Resolve a normalized absolute path to its inode's precomputed scope_key.
	 *
	 * Store-only helper intended for consumers (e.g., locking) that must not
	 * re-implement hashing/ltree codecs.
	 */
	public Optional<String> resolveScopeKey(String nodePath) {
		Objects.requireNonNull(nodePath, "nodePath");
		String scopeKey = scopeKeyForPath(nodePath);
		return inodeRepo.findIdByScopeKey(scopeKey).isPresent() ? Optional.of(scopeKey) : Optional.empty();
	}
    
	public Optional<String> resolvePathFromInode(Long inodeId) {
        Objects.requireNonNull(inodeId, "inodeId");

		Optional<InodeEntity> inodeOpt = inodeRepo.findById(inodeId);
		if (inodeOpt.isEmpty()) return Optional.empty();
		
		InodeEntity inode = inodeOpt.get();
		InodeEntity root = rootInode();
		
		if (inode.getId().equals(root.getId())) return Optional.of("/");
		
		List<InodePathSegmentEntity> segs = pathSegRepo.findAllByInode_IdOrderByOrdAsc(inode.getId());
		if (segs.isEmpty()) {
			// No backfill/repair in Phase-1: projection must exist for this inode.
			return Optional.empty();
		}
		
		List<String> names = new ArrayList<>(segs.size());
		for (InodePathSegmentEntity s : segs) {
			names.add(s.getDirEntry().getName());
		}
		return Optional.of("/" + String.join("/", names));
    }

	public String scopeKeyForPath(String path) {
	    return PathKeyCodec.toLtreeKey(path);
	}
	

    // -----------------------------
    // inode path resolution / creation (no full-path column)
    // -----------------------------

    public InodeEntity resolveOrCreateInode(String nodePath) {
        List<String> segs = PathUtils.splitSegments(nodePath);
        InodeEntity cur = rootInode();

        for (String seg : segs) {
            Optional<DirEntryEntity> e = dirRepo.findByParent_IdAndName(cur.getId(), seg);
            if (e.isPresent()) {
                cur = e.get().getChild();
                continue;
            }
            String childScopeKey = PathKeyCodec.appendSegLabel(cur.getScopeKey(), PathKeyCodec.label(seg));
            
            InodeEntity child = inodeRepo.save(new InodeEntity(Instant.now(), childScopeKey));
			DirEntryEntity entry = dirRepo.save(new DirEntryEntity(cur, seg, child));
			
			// Store-owned projection (Phase-1): inode -> ordered direntry chain
			List<InodePathSegmentEntity> parentSegs = pathSegRepo.findAllByInode_IdOrderByOrdAsc(cur.getId());
			List<InodePathSegmentEntity> childSegs = new ArrayList<>(parentSegs.size() + 1);
			for (InodePathSegmentEntity ps : parentSegs) {
				childSegs.add(new InodePathSegmentEntity(child, ps.getOrd(), ps.getDirEntry()));
			}
			childSegs.add(new InodePathSegmentEntity(child, parentSegs.size(), entry));
			pathSegRepo.saveAll(childSegs);
			
            cur = child;
        }
        return cur;
    }

    public void ensureRootInode() {
        // Store-owned bootstrap: root inode is the unique inode with scope_key = PathKeyCodec.ROOT_LABEL.
        // Also ensure the store-level invariant index exists (equality lookup by scope_key).
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_vn_inode_scope_key ON vn_inode(scope_key)");

        if (inodeRepo.findIdByScopeKey(PathKeyCodec.ROOT_LABEL).isPresent()) {
            return;
        }

        inodeRepo.save(new InodeEntity(Instant.now(), PathKeyCodec.ROOT_LABEL));
        inodeRepo.flush();
    }

}
