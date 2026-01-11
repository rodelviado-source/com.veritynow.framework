package com.veritynow.v2.store.core.jpa;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class InodeUtils {
	
	public static InodeEntity rootInode(InodeRepository inodeRepo) {
        // Use first inode row as root; if you prefer explicit root ID, enforce in migration SQL.
        return inodeRepo.findAll().stream().findFirst().orElseThrow();
    }

	public static Optional<Long> resolveInodeId(String nodePath, InodeRepository inodeRepo, DirEntryRepository dirRepo) {
        List<String> segs = PathUtils.splitSegments(nodePath);
        InodeEntity cur = rootInode(inodeRepo);

        for (String seg : segs) {
            Optional<DirEntryEntity> e = dirRepo.findByParent_IdAndName(cur.getId(), seg);
            if (e.isEmpty()) return Optional.empty();
            cur = e.get().getChild();
        }
        return Optional.of(cur.getId());
    }
    
	public static Optional<String> resolvePathFromInode(Long inodeId, InodeRepository inodeRepo, DirEntryRepository dirRepo) {
        Objects.requireNonNull(inodeId, "inodeId");

        Optional<InodeEntity> curOpt = inodeRepo.findById(inodeId);
        if (curOpt.isEmpty()) return Optional.empty();

        InodeEntity cur = curOpt.get();
        InodeEntity root = rootInode(inodeRepo);

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


    public static InodeEntity resolveOrCreateInode(String nodePath, InodeRepository inodeRepo, DirEntryRepository dirRepo) {
        List<String> segs = PathUtils.splitSegments(nodePath);
        InodeEntity cur = rootInode(inodeRepo);

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


}
