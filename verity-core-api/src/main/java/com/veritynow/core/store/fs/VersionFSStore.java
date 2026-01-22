package com.veritynow.core.store.fs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import com.veritynow.core.store.ImmutableBackingStore;
import com.veritynow.core.store.StoreOperation;
import com.veritynow.core.store.VersionStore;
import com.veritynow.core.store.base.AbstractStore;
import com.veritynow.core.store.base.PK;
import com.veritynow.core.store.base.PathEvent;
import com.veritynow.core.store.base.StoreContext;
import com.veritynow.core.store.db.PathUtils;
import com.veritynow.core.store.meta.BlobMeta;
import com.veritynow.core.store.meta.VersionMeta;

import util.FSUtil;
import util.JSON;

/**
 * Filesystem-backed VersionStore.
 *
 * Index layout (under root/Index):
 *   <nodePath>/
 *     HEAD                  -> contains newest version filename (ufn)
 *     <ufn1>                -> contains hash of VersionMeta JSON blob
 *     <ufn2>                -> contains hash of VersionMeta JSON blob
 *
 * Immutable backing store stores:
 *   - payload blobs (user content) as BlobMeta keyed by content hash
 *   - VersionMeta JSON blobs (binding: blob + event) also keyed by hash
 *
 * This class intentionally does NOT persist "Meta" or embed Event into blob-meta.
 * Versions are first-class VersionMeta records.
 */
public class VersionFSStore
        extends AbstractStore<PK, BlobMeta>
        implements VersionStore<PK, BlobMeta, VersionMeta> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionFSStore.class);

    private static final String HEAD = "HEAD";
    private static final String VERSION_META_NAME = "version.json";
    private static final String VERSION_META_MIME = "application/json";

    private final Path pathIndexDirectory;
    private final ImmutableBackingStore<String, BlobMeta> backingStore;

    public VersionFSStore(Path rootDirectory, ImmutableBackingStore<String, BlobMeta> backingStore) {
    	super(backingStore.getHashingService());
        Objects.requireNonNull(rootDirectory, "rootDirectory");
        Objects.requireNonNull(backingStore, "backingStore");

        this.pathIndexDirectory = rootDirectory.resolve("Index");
        this.backingStore = backingStore;

        try {
            Files.createDirectories(this.pathIndexDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        LOGGER.info(
                "\n\tFilesystem-backed Versioning Store started.\n\tIndexDir={}\n\tImmutableStore={}",
                this.pathIndexDirectory,
                backingStore.getClass().getName()
        );
    }

    
   
	private void moveHeads(VersionMeta... vms) throws IOException {
    	
    	for (VersionMeta vm : vms) {
    		byte[] bytes = JSON.MAPPER.writeValueAsBytes(vm);
    		
    		String vmHash = getHashingService().hash(new ByteArrayInputStream(bytes), false);

            String ufn = uniqVersionFilename(vm.operation(), vm.timestamp());
            Path nodeDir = nodeIndexDir(vm.path());

            writeToFile(nodeDir.resolve(ufn), vmHash);
            writeToFile(nodeDir.resolve(HEAD), ufn);
    	}
    	
	}


	@Override
    public Optional<InputStream> getContent(PK key) {
    	Objects.requireNonNull(key, "key");
    	Objects.requireNonNull(key.hash(), "hash");
        try {
            return backingStore.retrieve(key.hash());
        } catch (IOException e) {
            LOGGER.error("Unable to retrieve hash={}", key.hash(), e);
            return Optional.empty();
        }
    }

    // ---------------------------------------------------------------------
    // Existence
    // ---------------------------------------------------------------------

    @Override
    public boolean exists(PK key) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");

        String nodePath = normalizePath(key.path());
        Path nodeDir = nodeIndexDir(nodePath);
        return Files.exists(nodeDir.resolve(HEAD));
    }

    // ---------------------------------------------------------------------
    // Read versions
    // ---------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Optional<VersionMeta> getLatestVersion(String nodePath) throws IOException {
        Objects.requireNonNull(nodePath, "nodePath");
        nodePath = normalizePath(nodePath);

        Path nodeDir = nodeIndexDir(nodePath);
        Path headFile = nodeDir.resolve(HEAD);

        if (!Files.exists(headFile) || !Files.isRegularFile(headFile)) {
            return Optional.empty();
        }

        String ufn = readFromFile(headFile).orElse(null);
        if (ufn == null || ufn.isBlank()) return Optional.empty();

        Path verFile = nodeDir.resolve(ufn);
        if (!Files.exists(verFile) || !Files.isRegularFile(verFile)) return Optional.empty();

        String versionMetaHash = readFromFile(verFile).orElse(null);
        if (versionMetaHash == null || versionMetaHash.isBlank()) return Optional.empty();

        return Optional.of(loadVersionMetaByHash(versionMetaHash));
    }

    @Override
    @Transactional
    public Optional<InputStream> read(PK key) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");

        String nodePath = PathUtils.normalizePath(key.path());
        Optional<VersionMeta> opt = getLatestVersion(nodePath);

        if (opt.isPresent()) {
        	VersionMeta mx = opt.get();
            if (!isDeleted(mx)) {
                return backingStore.retrieve(mx.hash());
            }
            LOGGER.info("Attempt to read a deleted blob at path {}", nodePath);
        }
        
        LOGGER.warn("No version of blob exists at path {}", nodePath);
        return Optional.empty();
    }
    
	@Override
    @Transactional(readOnly = true)
    public List<VersionMeta> getAllVersions(String nodePath) throws IOException {
        Objects.requireNonNull(nodePath, "nodePath");
        nodePath = normalizePath(nodePath);

        Path nodeDir = nodeIndexDir(nodePath);
        Path headFile = nodeDir.resolve(HEAD);

        // Preserve FS semantics: non-leaf/container returns empty versions
        if (!Files.exists(headFile) || !Files.isRegularFile(headFile)) {
            return List.of();
        }

        List<Path> versionFiles = listVersionFiles(nodeDir);

        List<VersionMeta> out = new ArrayList<>(versionFiles.size());
        for (Path vf : versionFiles) {
            String versionMetaHash = readFromFile(vf).orElse(null);
            if (versionMetaHash == null || versionMetaHash.isBlank()) continue;
            out.add(loadVersionMetaByHash(versionMetaHash));
        }
        return out;
    }

    // ---------------------------------------------------------------------
    // Tree navigation
    // ---------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<VersionMeta> list(String nodePath) throws IOException {
        Objects.requireNonNull(nodePath, "nodePath");
        nodePath = normalizePath(nodePath);

        List<String> children = listChildren(nodePath);
        List<VersionMeta> out = new ArrayList<>(children.size());

        for (String child : children) {
            String p = joinPath(nodePath, child);
            Optional<VersionMeta> latest = getLatestVersion(p);
            latest.stream().forEachOrdered(out::add);
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listChildren(String nodePath) throws IOException {
        Objects.requireNonNull(nodePath, "nodePath");
        nodePath = normalizePath(nodePath);

        Path dir = nodeIndexDir(nodePath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) return List.of();

        // Children are subdirectories under the node's index directory.
        // (Version files live alongside HEAD; they are regular files, not directories.)
        List<String> out = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                if (Files.isDirectory(p)) {
                    out.add(p.getFileName().toString());
                }
            }
        }
        out.sort(String::compareTo);
        return out;
    }

    // ---------------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------------

    @Override
    @Transactional
    public Optional<BlobMeta> create(PK key, BlobMeta meta, InputStream content) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");
        Objects.requireNonNull(meta, "meta");
        Objects.requireNonNull(content, "content");

        String nodePath = normalizePath(key.path());
        return createNewVersionAtExactPath(nodePath, meta, content, StoreOperation.Created());
    }

    /**
     * CREATE - create a new version under path/{id}
     * If id is null, generates a UUID and uses it as the last segment.
     */
    @Override
    @Transactional
    public Optional<BlobMeta> create(PK key, BlobMeta meta, InputStream content, String id) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");
        Objects.requireNonNull(meta, "meta");
        Objects.requireNonNull(content, "content");

        String base = normalizePath(key.path());
        String seg = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id;
        String nodePath = joinPath(base, seg);

        return createNewVersionAtExactPath(nodePath, meta, content, StoreOperation.Created());
    }

    // ---------------------------------------------------------------------
    // Update / Delete / Undelete / Restore
    // ---------------------------------------------------------------------

    @Override
    @Transactional
    public Optional<BlobMeta> update(PK key, InputStream content) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");
        Objects.requireNonNull(content, "content");

        String nodePath = normalizePath(key.path());

        Optional<VersionMeta> opt = getLatestVersion(nodePath);
        if (opt.isEmpty()) {
            LOGGER.warn("Cannot update non-existent path {}", nodePath);
            return Optional.empty();
        }
        VersionMeta latest = opt.get();
        if (isDeleted(latest)) {
            LOGGER.warn("Cannot update deleted path {}", nodePath);
            return Optional.empty();
        }

        // Reuse the existing blob meta attributes (name/mime), only content changes.
        BlobMeta prev = latest.blobMeta();
        BlobMeta nextAttr = new BlobMeta(null, prev.name(), prev.mimeType(), 0L);

        return appendVersion(nodePath, nextAttr, content, StoreOperation.Updated());
    }

    @Override
    @Transactional
    public Optional<BlobMeta> delete(PK key) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");

        String nodePath = normalizePath(key.path());

        Optional<VersionMeta> opt = getLatestVersion(nodePath);
        if (opt.isEmpty()) {
            LOGGER.warn("Cannot delete non-existent path {}", nodePath);
            return Optional.empty();
        }
        VersionMeta latest = opt.get();
        if (isDeleted(latest)) {
            return latest.blobMeta() != null ? Optional.of(latest.blobMeta()) : Optional.empty();
        }

        // Tombstone version: keep last blob meta (or zero-size marker), record delete event.
        BlobMeta prev = latest.blobMeta();
        BlobMeta tombstone = (prev == null)
                ? new BlobMeta(null, null, null, 0L)
                : new BlobMeta(prev.hash(), prev.name(), prev.mimeType(), prev.size());

        // No payload write needed on delete; we only add a VersionMeta record.
        appendVersionMetaOnly(nodePath, tombstone, StoreOperation.Deleted());
        return Optional.of(tombstone);
    }

    @Override
    @Transactional
    public Optional<BlobMeta> undelete(PK key) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");

        String nodePath = normalizePath(key.path());

        Optional<VersionMeta> opt = getLatestVersion(nodePath);
        if (opt.isEmpty()) {
            LOGGER.warn("Cannot undelete non-existent path {}", nodePath);
            return Optional.empty();
        }
        VersionMeta latest = opt.get();
        if (!isDeleted(latest)) {
            return Optional.of(latest.blobMeta());
        }

        // Undelete by creating an UNDELETE event. Blob stays the same as latest (tombstone or previous).
        BlobMeta blob = latest.blobMeta();
       

        appendVersionMetaOnly(nodePath, blob, StoreOperation.Undeleted());
        return Optional.of(blob);
    }

    @Override
    @Transactional
    public Optional<BlobMeta> restore(PK key) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(key.path(), "key.path");
        Objects.requireNonNull(key.hash(), "key.hash");

        String nodePath = normalizePath(key.path());
        String targetHash = key.hash();

        List<VersionMeta> versions = getAllVersions(nodePath);
        Optional<VersionMeta> target = versions.stream()
                .filter(vm -> vm != null && vm.blobMeta() != null && targetHash.equals(vm.blobMeta().hash()))
                .findFirst();

        if (target.isEmpty()) {
            LOGGER.warn("Cannot restore: version not found path={} hash={}", nodePath, targetHash);
            return Optional.empty();
        }

        // Restore is implemented as "move HEAD to the matching version filename".
        // Since our index is ufn -> versionMetaHash, we find the ufn that points to the desired VersionMeta hash.
        Path nodeDir = nodeIndexDir(nodePath);
        Path headFile = nodeDir.resolve(HEAD);
        if (!Files.exists(headFile)) return Optional.empty();

        // Find ufn by scanning version files (acceptable for now; optimize later if needed)
        String desiredBlobHash = targetHash;
        Optional<String> matchingUfn = findUfnByBlobHash(nodeDir, desiredBlobHash);

        if (matchingUfn.isEmpty()) {
            LOGGER.warn("Cannot restore: ufn not found for path={} hash={}", nodePath, targetHash);
            return Optional.empty();
        }

        writeToFile(headFile, matchingUfn.get());
        // Also emit a RESTORE event as a new version-meta record (optional but recommended)
        appendVersionMetaOnly(nodePath, target.get().blobMeta(), StoreOperation.Restored());

        return Optional.ofNullable(target.get().blobMeta());
    }

    // ---------------------------------------------------------------------
    // Internal: create/append
    // ---------------------------------------------------------------------

    private Optional<BlobMeta> createNewVersionAtExactPath(String nodePath, BlobMeta attr, InputStream content, String op)
            throws IOException {

        // Create parent directories in index (filesystem creates parents implicitly)
        ensureNodeDir(nodePath);

        // Disallow create if already exists (matching typical FS "create new leaf" semantics)
        Optional<VersionMeta> existing = getLatestVersion(nodePath);
        if (existing.isPresent() && !isDeleted(existing.get())) {
            LOGGER.warn("Path already exists {}", nodePath);
            return Optional.empty();
        }

        return appendVersion(nodePath, attr, content, op);
    }

    private Optional<BlobMeta> appendVersion(String nodePath, BlobMeta attr, InputStream content, String op) throws IOException {
        Objects.requireNonNull(nodePath, "nodePath");
        Objects.requireNonNull(attr, "attr");
        Objects.requireNonNull(content, "content");

        // 1) Persist payload blob to immutable store
        BlobMeta storedPayload = backingStore.save(attr, content)
                .orElseThrow(() -> new IOException("Unable to persist payload blob"));

        // 2) Create PathEvent + VersionMeta binding
        VersionMeta vm = new VersionMeta(
                storedPayload,
                createPathEvent(nodePath, op)
        );
        
        // 3) Persist VersionMeta JSON blob and index it
       persistVersionMeta(vm);
       moveHeads(vm);

        return Optional.of(storedPayload);
    }

    private void appendVersionMetaOnly(String nodePath, BlobMeta blob, String op) throws IOException {
        Objects.requireNonNull(nodePath, "nodePath");

        VersionMeta vm = new VersionMeta(
                blob,
                createPathEvent(nodePath, op)
        );

        String vmHash = persistVersionMeta(vm);
        String ufn = uniqVersionFilename(vm.operation(), vm.timestamp());

        Path nodeDir = nodeIndexDir(nodePath);
        ensureNodeDir(nodePath);

        writeToFile(nodeDir.resolve(ufn), vmHash);
        writeToFile(nodeDir.resolve(HEAD), ufn);
    }

    private String persistVersionMeta(VersionMeta vm) throws IOException {
        byte[] bytes = JSON.MAPPER.writeValueAsBytes(vm);

        BlobMeta meta = new BlobMeta(
                null,
                VERSION_META_NAME,
                VERSION_META_MIME,
                bytes.length
        );

        BlobMeta stored = backingStore.save(meta, new ByteArrayInputStream(bytes))
                .orElseThrow(() -> new IOException("Unable to persist VersionMeta blob"));

        return stored.hash();
    }

    private VersionMeta loadVersionMetaByHash(String versionMetaHash) throws IOException {
        try (InputStream is = backingStore.retrieve(versionMetaHash)
                .orElseThrow(() -> new IOException("Missing VersionMeta blob: " + versionMetaHash))) {
            return JSON.MAPPER.readValue(is, VersionMeta.class);
        }
    }

    // ---------------------------------------------------------------------
    // Internal: event construction
    // ---------------------------------------------------------------------

    private PathEvent createPathEvent(String nodePath, String op) {
        StoreContext sc = StoreContext.create(op);
        return new PathEvent(nodePath , sc);
    }

    // ---------------------------------------------------------------------
    // Internal: index directory helpers
    // ---------------------------------------------------------------------

    private Path nodeIndexDir(String nodePath) {
        // nodePath is normalized with leading "/"
        String rel = nodePath.startsWith("/") ? nodePath.substring(1) : nodePath;
        if (rel.isBlank()) return pathIndexDirectory; // root
        return pathIndexDirectory.resolve(rel);
    }

    private void ensureNodeDir(String nodePath) throws IOException {
        Path dir = nodeIndexDir(nodePath);
        Files.createDirectories(dir);
    }

    private static List<Path> listVersionFiles(Path nodeDir) throws IOException {
        List<Path> out = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(nodeDir)) {
            for (Path p : ds) {
                if (!Files.isRegularFile(p)) continue;
                String name = p.getFileName().toString();
                if (HEAD.equals(name)) continue;
                out.add(p);
            }
        }
        out.sort(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed());
        return out;
    }

    private Optional<String> findUfnByBlobHash(Path nodeDir, String blobHash) throws IOException {
        List<Path> versionFiles = listVersionFiles(nodeDir);
        for (Path vf : versionFiles) {
            String vmHash = readFromFile(vf).orElse(null);
            if (vmHash == null || vmHash.isBlank()) continue;
            VersionMeta vm = loadVersionMetaByHash(vmHash);
            if (vm != null && vm.blobMeta() != null && blobHash.equals(vm.blobMeta().hash())) {
                return Optional.of(vf.getFileName().toString());
            }
        }
        return Optional.empty();
    }

    //Expected to read as is no trimming (used for writing hash/filname and is used as references)
    private static Optional<String> readFromFile(Path file) throws IOException {
        if (!Files.exists(file) || !Files.isRegularFile(file)) return Optional.empty();
        String line =  Files.readString(file, StandardCharsets.UTF_8);
        if (line == null) {
        	Optional.empty();
        }
        return Optional.of(line);
    }

    //Expected to write as is no trimming (used for writing hash/filname and is used as references)
    private static void writeToFile(Path file, String hash) throws IOException {
        Files.createDirectories(file.getParent());
        if (hash != null)
        	FSUtil.safeWrite(file, hash);
    }

    private static String joinPath(String parent, String child) {
        parent = normalizePath(parent);
        if (parent.equals("/")) return "/" + child;
        return parent + "/" + child;
    }

    private static String normalizePath(String p) {
        if (p == null) return null;
        return p.startsWith("/") ? p : ("/" + p);
    }

    private static String uniqVersionFilename(String o, long timestamp) {
        // FS ordering + uniqueness:
        //   <timestamp>_<uuid>_<op>
        // Keep it stable and lexicographically sortable by timestamp when padded.
        // If you need fixed-width timestamps, pad to 13 digits.
        String ts = String.format("%013d", timestamp);
        String op = safe(o);
        return ts + "_" + UUID.randomUUID() + "_" + op;
    }

    private static String safe(String s) {
        if (s == null) return "null";
        // Keep filenames simple
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static boolean isDeleted(VersionMeta m) {
        return StoreOperation.Deleted().equals(m.pathEvent().operation());
    }
}
