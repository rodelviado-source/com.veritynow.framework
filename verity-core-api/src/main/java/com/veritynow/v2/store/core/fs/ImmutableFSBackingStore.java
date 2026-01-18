package com.veritynow.v2.store.core.fs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.Tika;

import com.fasterxml.jackson.core.type.TypeReference;
import com.veritynow.v2.store.HashingService;
import com.veritynow.v2.store.ImmutableBackingStore;
import com.veritynow.v2.store.core.AbstractStore;
import com.veritynow.v2.store.meta.BlobMeta;

import util.FSUtil;
import util.JSON;

public class ImmutableFSBackingStore extends AbstractStore<String, BlobMeta>
		implements ImmutableBackingStore<String, BlobMeta> {

	private final Path blobDirectory;

	private static final Logger LOGGER = LogManager.getLogger();

	Tika tika = new Tika();

	public ImmutableFSBackingStore(Path rootDirectory, HashingService hs) {
		super(hs);
		this.blobDirectory = rootDirectory.resolve("Blobs");
		LOGGER.info("\n\tImmutable Filesystem backed Store started\n\tRoot Directory at " + blobDirectory);
	}

	@Override
	public boolean exists(String hash) throws IOException {
		Objects.requireNonNull(hash, "hash");
		Path p = getBlobPath(hash, false);
		return Files.exists(p);
	}

	@Override
	public Optional<BlobMeta> save(String name, String mimetype, InputStream is) throws IOException {
		return create(null, new BlobMeta(null, name, mimetype, 0l), is);
	}

	// ----------------------------
	// CREATE
	//
	// ----------------------------
	@Override
	public Optional<BlobMeta> create(String key, BlobMeta meta, InputStream content) throws IOException {

		Objects.requireNonNull(content, "content");

		// Get all derivable truth, The store is the authority for these attributes

		try {
			// Compute hash and cache the contents
			// Since we are already reading the content, we migh as well cache it
			String hash = getHashingService().hash(content, true);

			// Get the actual size of the content
			long size = getHashingService().size().orElseThrow(() -> {
				return new IOException("Zero content size");
			});

			// Returns a sharded path with hash as the filename
			// Set createParentDir to true
			Path blobPath = getBlobPath(hash, true);
			// Create a path for our json meta, lives alongside glob
			Path metaPath = getBlobPath(hash + JSON_EXTENSION, false);

			// just return the stored meta if it exist
			if (Files.exists(metaPath)) {
				return Optional.of(JSON.MAPPER.readValue(Files.readAllBytes(metaPath), new TypeReference<BlobMeta>() {
				}));
			}

			// get a store ID and timestamp
			String storeId = UUID.randomUUID().toString();

			// Overridable attributes
			// Set attributes that was passed in meta
			// If not provided use sane defaults

			Optional<byte[]> header = getHashingService().header();
			String mimeType = meta.mimeType() != null ? meta.mimeType()
					: header.isPresent() ? tika.detect(header.get()) : "application/octet-stream";
			String name = meta.name() != null ? meta.name() : storeId;

			// all ready to create a new versions meta
			BlobMeta blobMeta = new BlobMeta(hash, name, mimeType, size);

			byte[] metaAsBytes = JSON.MAPPER.writeValueAsBytes(blobMeta);

			// save the actual payload
			if (!Files.exists(blobPath)) {
				// get the cached content, set delete on close
				Optional<InputStream> isOpt = getHashingService().getInputStream(true);
				if (isOpt.isEmpty()) {
					throw new IOException("Unable to read content");
				}
				try (InputStream is = isOpt.get()) {
					FSUtil.safeWrite(blobPath, is);
					FSUtil.safeWrite(metaPath, metaAsBytes);
				}
			}

			return Optional.of(blobMeta);
		} finally {
			// delete the cache
			Optional<InputStream> isOpt = getHashingService().getInputStream(true);
			if (isOpt.isPresent())
				try (InputStream is = isOpt.get()) {
				} catch (Exception e) {
				}
			;
		}
	}
	
	

	@Override
	public Optional<BlobMeta> create(String key, BlobMeta blob, InputStream in, String id) throws IOException {
		//ignore given ID
		return create(key, blob, in);
	}

	@Override
	public Optional<BlobMeta> update(String hash, InputStream content) throws IOException {
		Objects.requireNonNull(hash, "hash");
		Objects.requireNonNull(content, "content");
		return readOnly(hash, content);
	}

	@Override
	public Optional<InputStream> read(String hash) throws IOException {
		Objects.requireNonNull(hash, "hash");
		return retrieve(hash);
	}

	@Override
	public Optional<BlobMeta> delete(String hash) throws IOException {
		Objects.requireNonNull(hash, "hash");
		return readOnly(hash, null);
	}

	@Override
	public Optional<BlobMeta> undelete(String hash) throws IOException {
		Objects.requireNonNull(hash, "hash");
		return readOnly(hash, null);
	}

	@Override
	public Optional<BlobMeta> restore(String hash) throws IOException {
		Objects.requireNonNull(hash, "hash");
		return readOnly(hash, null);
	}

	@Override
	public Optional<BlobMeta> save(BlobMeta meta, InputStream is) throws IOException {
		return create(null, meta, is);
	}

	@Override
	public Optional<InputStream> retrieve(String hash) throws IOException {
		InputStream b = getBlob(hash);
		if (b != null)
			return Optional.of(b);
		return Optional.empty();
	}

	/** HELPERS DOWN HERE **/

	private Path getBlobPath(String hash, boolean createParentDir) throws IOException {
		// git-like sharding: /ab/<hash>
		String shard = hash.substring(0, 2);
		Path shardDir = blobDirectory.resolve(shard);
		if (createParentDir && !Files.exists(shardDir)) {
			Files.createDirectories(shardDir);
		}
		return shardDir.resolve(hash);

	}

	private InputStream getBlob(String hash) throws IOException {
		Path p = getBlobPath(hash, false);
		if (!Files.exists(p))
			return null;
		return Files.newInputStream(p);
	}

	// A no write operation but returns a sensible Meta
	private Optional<BlobMeta> readOnly(String hash, InputStream content) throws IOException {

		// Get all derivable truth, The store is the authority for these attributes
		if (content != null) {
			hash = getHashingService().hash(content, false);
		}

		if (hash == null)
			return Optional.empty();

		// Create a path for our json meta
		Path metaPath = getBlobPath(hash + JSON_EXTENSION, false);

		// just return the stored meta if it exist
		if (Files.exists(metaPath)) {
			// let it throw an exception if it fails
			return Optional.of(JSON.MAPPER.readValue(Files.readAllBytes(metaPath), new TypeReference<BlobMeta>() {
			}));
		}

		// does not exists we just return an empty Meta
		// no truth to behold
		return Optional.empty();
	}

}
