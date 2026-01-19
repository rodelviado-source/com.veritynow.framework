package com.veritynow.v2.store.core.jpa;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;

public class InodeManager {
	
	private JdbcTemplate jdbc;
	private InodeRepository inodeRepo;
	private DirEntryRepository dirRepo;
	private InodePathSegmentRepository pathSegRepo;
	private VersionMetaHeadRepository headRepo;
	private VersionMetaRepository verRepo;

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
		  Long rootId = jdbc.queryForObject(
		      "select inode_id from vn_root where singleton = TRUE",
		      Long.class
		  );
		  if (rootId == null) throw new IllegalStateException("vn_root missing singleton row");
		  return inodeRepo.findById(rootId).orElseThrow(() -> new IllegalStateException("Root inode missing id=" + rootId));
		}
	

	public Optional<Long> resolveInodeId(String nodePath) {
        List<String> segs = PathUtils.splitSegments(nodePath);
        InodeEntity cur = rootInode();

        for (String seg : segs) {
            Optional<DirEntryEntity> e = dirRepo.findByParent_IdAndName(cur.getId(), seg);
            if (e.isEmpty()) return Optional.empty();
            cur = e.get().getChild();
        }
        return Optional.of(cur.getId());
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
            String childScopeKey = PathKeyCodec.appendSegLabel(
                    cur.getScopeKey(),
                    PathKeyCodec.segLabelMd5_16(seg)
            );
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
  	  // If locking support is installed, it owns root via vn_root.
  	  // If vn_root is absent (locking not installed), fallback to the legacy "one inode exists" behavior.
  	  try {
  	    Integer hasRoot = jdbc.queryForObject(
  	        "select count(*) from vn_root where singleton = TRUE",
  	        Integer.class
  	    );
  	    if (hasRoot != null && hasRoot > 0) return;

  	    // Create inode via JPA (keeps entity lifecycle consistent)
	    // Root scope key is the empty ltree (matches vn_path_to_scope_key('/') behavior).
	    InodeEntity root = inodeRepo.save(new InodeEntity(Instant.now(), ""));
  	    inodeRepo.flush();

  	    // Register as root pointer (idempotent)
  	    jdbc.update(
  	        "insert into vn_root(singleton, inode_id) values (TRUE, ?) on conflict (singleton) do nothing",
  	        root.getId()
  	    );
  	  } catch (Exception e) {
  	    // vn_root table not present: locking support not installed.
  	    if (inodeRepo.count() == 0) {
	      inodeRepo.save(new InodeEntity(Instant.now(), ""));
  	      inodeRepo.flush();
  	    }
  	  }
  	}

}
