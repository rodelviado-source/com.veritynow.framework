package com.veritynow.core.store.immutablestore;

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

import com.veritynow.core.store.HashingService;
import com.veritynow.core.store.ImmutableStore;
import com.veritynow.core.store.base.AbstractStore;
import com.veritynow.core.store.meta.BlobMeta;
import com.veritynow.core.store.persistence.jooq.tables.records.VnBlobRecord;

import util.FSUtil;
import util.JSON;

public class ImmutableFSBackingStore extends AbstractStore<String, BlobMeta>
		implements ImmutableStore<String, BlobMeta> {

	private final Path blobDirectory;
	private final String algo;
	private static final Logger LOGGER = LogManager.getLogger();
	private final ImmutableRepository repo;

	Tika tika = new Tika();

	public ImmutableFSBackingStore(Path rootDirectory, ImmutableRepository repo, HashingService hs) {
		super(hs);
		this.blobDirectory = rootDirectory.resolve("Blobs");
		this.repo = repo;
		this.algo = hs.getAlgorithm();
		LOGGER.info("Immutable Filesystem({}) backed Store started.	Root Directory at {}", algo, blobDirectory);
	}

	@Override
	public boolean exists(String hash) throws IOException {
		Objects.requireNonNull(hash, "hash");
		Objects.requireNonNull(algo, "algorithm");
		return repo.exists(hash, algo);
	}

	@Override
	public Optional<BlobMeta> save(String name, String mimetype, InputStream is) throws IOException {
		return create(null, new BlobMeta(name, mimetype), is);
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
			
			Optional<BlobMeta> result = repo.withHashLockTx(hash, algo, (dsl) ->  {
		
				
				Optional<VnBlobRecord> metaRepo = repo.findByHashAndAlgo(dsl, hash, algo);
				// just return the stored meta if it exist
				//but first check for collision
				if (metaRepo.isPresent()) {
					BlobMeta bm = toBlobMeta(metaRepo.get());
					// check if there is a hash collsion
					if (bm.size() != size || !(hash.equals(bm.hash()) && algo.equals(bm.hashAlgorithm()))) {
						try (InputStream is = getHashingService().getInputStream(true).get()) {
							// store the blob for forensic analysis
							FSUtil.safeWrite(blobPath.getParent().resolve(hash + "-collided"), is);
						} catch (Throwable e) {
						}
						;
						LOGGER.error("Collission detected computed[hash({}) size({})],  current[stored {}]", hash, size,
								JSON.MAPPER.writeValueAsString(bm));
						throw new IOException("Collission detected");
					}
					// no collison
					return Optional.of(bm);
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
				BlobMeta blobMeta = new BlobMeta(algo, hash, name, mimeType, size);
				VnBlobRecord br = toVnBlobRecord(blobMeta);

				// save the actual payload
				// get the cached content, set delete on close
				Optional<InputStream> isOpt = getHashingService().getInputStream(true);
				if (isOpt.isEmpty()) {
					throw new IOException("Unable to read content");
				}
				
				
				try (InputStream is = isOpt.get()) {
					FSUtil.safeWrite(blobPath, is);
					// only publish if write succeeds
					repo.insert(dsl, br);
				} 
				
				return Optional.of(blobMeta);
			 
			});
			return result;
			
		} finally {
			// delete the cache
			Optional<InputStream> isOpt = getHashingService().getInputStream(true);
			if (isOpt.isPresent())
				try (@SuppressWarnings("unused")
				InputStream is = isOpt.get()) {
				} catch (Exception e) {
				}
		}
	}

	@Override
	public Optional<BlobMeta> create(String key, BlobMeta blob, InputStream in, String id) throws IOException {
		// ignore given ID
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
		if (repo.exists(hash, algo)) {
			InputStream b = getBlob(hash, algo);
			if (b != null)
				return Optional.of(b);
		}
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
		return shardDir.resolve(hash + "-" + algo);
	}

	

	private InputStream getBlob(String hash, String algo) throws IOException {
		if (repo.exists(hash, algo)) {
			Path p = getBlobPath(hash, false);
			if (!Files.exists(p))
				return null;
			return Files.newInputStream(p);
		}
		return null;
	}

	// A no write operation but returns a sensible Meta
	private Optional<BlobMeta> readOnly(String hash, InputStream content) throws IOException {

		// Get all derivable truth, The store is the authority for these attributes
		if (content != null) {
			hash = getHashingService().hash(content, false);
		}

		if (hash == null)
			return Optional.empty();
		
		Optional<VnBlobRecord> br = repo.findByHashAndAlgo(hash, algo);

		if (br.isPresent()) return Optional.of(toBlobMeta(br.get()));

		// does not exists we just return an empty Meta
		// no truth to behold
		return Optional.empty();
	}

	private BlobMeta toBlobMeta(VnBlobRecord br) {
		return new BlobMeta(br.getHashAlgorithm(), br.getHash(), br.getName(), br.getMimeType(), br.getSize());
	}

	private VnBlobRecord toVnBlobRecord(BlobMeta bm) {
		VnBlobRecord vr = new VnBlobRecord();
		vr.setHash(bm.hash());
		vr.setHashAlgorithm(bm.hashAlgorithm());
		vr.setMimeType(bm.mimeType());
		vr.setName(bm.name());
		vr.setSize(bm.size());
		return vr;
	}

	@Override
	public Optional<BlobMeta> getMeta(String key) throws IOException {
		return readOnly(key, null);
	}
}
