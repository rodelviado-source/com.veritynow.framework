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
	private VersionMetaHeadRepository headRepo;
	private VersionMetaRepository verRepo;

	public InodeManager(JdbcTemplate jdbc, InodeRepository inodeRepo, DirEntryRepository dirRepo, VersionMetaHeadRepository headRepo, VersionMetaRepository verRepo) {
		Objects.requireNonNull(jdbc, "JDBC required");
		Objects.requireNonNull(inodeRepo, InodeEntity.class.getName() + " Repository required");
		Objects.requireNonNull(dirRepo,   DirEntryEntity.class.getName() + " Repository required");
		Objects.requireNonNull(headRepo,  VersionMetaHeadEntity.class.getName() + " Repository required");
		Objects.requireNonNull(verRepo,   VersionMetaEntity.class.getName() + " Repository required");
		
		this.jdbc = jdbc;
		this.inodeRepo = inodeRepo;
		this.dirRepo = dirRepo;
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

        Optional<InodeEntity> curOpt = inodeRepo.findById(inodeId);
        if (curOpt.isEmpty()) return Optional.empty();

        InodeEntity cur = curOpt.get();
        InodeEntity root = rootInode();

        // Root path
        if (cur.getId().equals(root.getId())) {
            return Optional.of("/");
        }

        List<String> segments = new ArrayList<>();

        while (!cur.getId().equals(root.getId())) {
            Optional<DirEntryEntity> entryOpt =
            		dirRepo.findByChild_Id(cur.getId());

            if (entryOpt.isEmpty()) {
                // Orphan inode: violates tree invariant
                return Optional.empty();
            }

            DirEntryEntity entry = entryOpt.get();
            segments.add(entry.getName());
            cur = entry.getParent();

            if (cur == null) {
                // Defensive: corrupted graph
                return Optional.empty();
            }
        }

        Collections.reverse(segments);
        return Optional.of("/" + String.join("/", segments));
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
            InodeEntity child = inodeRepo.save(new InodeEntity(Instant.now()));
            dirRepo.save(new DirEntryEntity(cur, seg, child));
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
  	    InodeEntity root = inodeRepo.save(new InodeEntity(Instant.now()));
  	    inodeRepo.flush();

  	    // Register as root pointer (idempotent)
  	    jdbc.update(
  	        "insert into vn_root(singleton, inode_id) values (TRUE, ?) on conflict (singleton) do nothing",
  	        root.getId()
  	    );
  	  } catch (Exception e) {
  	    // vn_root table not present: locking support not installed.
  	    if (inodeRepo.count() == 0) {
  	      inodeRepo.save(new InodeEntity(Instant.now()));
  	      inodeRepo.flush();
  	    }
  	  }
  	}

}
