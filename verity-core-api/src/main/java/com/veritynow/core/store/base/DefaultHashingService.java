package com.veritynow.core.store.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.util.xxhash.XXH128Hash;
import org.lwjgl.util.xxhash.XXH3State;
import org.lwjgl.util.xxhash.XXHash;


import org.springframework.beans.factory.annotation.Value;


import com.veritynow.core.store.HashingService;

/**
 * HashingService implementation supporting:
 *  - XXH3 128-bit via LWJGL native xxHash bindings (streaming: reset/update/digest)
 *  - Any MessageDigest algorithm (e.g., SHA-256) for fallback
 *
 * Design goals:
 *  - No full-content heap load
 *  - Preserve existing cacheContent behavior (optional spooling for later re-read)
 *  - Maintain header + size tracking
 *
 * Notes:
 *  - XXH3State is stored per-thread (ThreadLocal) to avoid allocating/freeing native state per call.
 *  - Chunk updates use a per-thread direct ByteBuffer scratch to satisfy LWJGL native calls efficiently.
 */
public class DefaultHashingService implements HashingService {

	public static int BUFFER_SIZE = 1024;

	private static final Logger LOGGER = LogManager.getLogger();

	private final boolean isXXH3;

	// For MessageDigest algorithms (SHA-256, etc.). Null when XXH3 mode is active.
	private final ThreadLocal<MessageDigest> digest;

	private final ThreadLocal<Long> size = ThreadLocal.withInitial(() -> -1L);
	private final ThreadLocal<String> hash = ThreadLocal.withInitial(() -> null);
	private final ThreadLocal<byte[]> header = ThreadLocal.withInitial(() -> null);

	private final ThreadLocal<Path> tempPath = new ThreadLocal<>();
	private final ThreadLocal<String> algorithm = new ThreadLocal<>();
	private final ThreadLocal<Boolean> cacheContent = ThreadLocal.withInitial(() -> false);
	private final ThreadLocal<Integer> bufferSize = ThreadLocal.withInitial(() -> BUFFER_SIZE);

	// LWJGL xxHash state + scratch buffers (per-thread)
	private final ThreadLocal<XXH3State> xxh3State = ThreadLocal.withInitial(() -> {
		XXH3State s = XXHash.XXH3_createState();
		if (s == null) {
			throw new IllegalStateException("XXH3_createState() returned null; LWJGL natives may be missing.");
		}
		return s;
	});

	private final ThreadLocal<ByteBuffer> directChunk = ThreadLocal.withInitial(() ->
		// allocateDirect once per thread; capacity is adjusted lazily if bufferSize changes
		ByteBuffer.allocateDirect(BUFFER_SIZE)
	);

	private final ThreadLocal<XXH128Hash> xxh128Result = ThreadLocal.withInitial(() ->
		// uses BufferUtils internally (direct buffer); result struct is re-used per thread
		XXH128Hash.create()
	);

	public DefaultHashingService(@Value("${verity.store.hash.algo:XXH3}") String algo) throws NoSuchAlgorithmException {
		this.isXXH3 = "XXH3".equalsIgnoreCase(algo);

		if (this.isXXH3) {
			this.digest = null;
		} else {
			this.digest = ThreadLocal.withInitial(() -> {
				try {
					return MessageDigest.getInstance(algo);
				} catch (NoSuchAlgorithmException e) {
					return null;
				}
			});
		}

		if (this.digest == null && !this.isXXH3) {
			throw new NoSuchAlgorithmException(algo);
		}

		LOGGER.info("Default hashing service using {} algorithm started ", algo);
		algorithm.set(algo.toLowerCase().trim());
	}

	@Override
	public String hash(Path contentPath) {
		Objects.requireNonNull(contentPath, "content");
		try {
			if (!Files.isRegularFile(contentPath)) {
				throw new RuntimeException("Path is not a regular file " + contentPath);
			}
			try (InputStream is = Files.newInputStream(contentPath)) {
				// No caching for Path hashing; this preserves the old behavior (streaming).
				return hash(is, false);
			}
		} catch (Exception e) {
			throw new RuntimeException("Error computing hash " + contentPath, e);
		}
	}

	@Override
	public String hash(InputStream content, boolean cacheContent) {
		Objects.requireNonNull(content, "content");

		this.cacheContent.set(cacheContent);

		if (cacheContent) {
			prepareTempFile();
		} else {
			// ensure any prior cached temp file in this thread does not leak across calls
			cleanupTempIfPresent();
			tempPath.set(null);
		}

		if (isXXH3) {
			return hashXxh3_128_streaming(content, cacheContent);
		}

		return hashMessageDigest_streaming(content, cacheContent);
	}

	private String hashXxh3_128_streaming(InputStream content, boolean cacheContent) {
		final XXH3State state = xxh3State.get();
		final XXH128Hash out = xxh128Result.get();

		// Ensure scratch direct buffer is at least bufferSize capacity
		ByteBuffer scratch = directChunk.get();
		int bs = bufferSize.get();
		if (scratch.capacity() < bs) {
			scratch = ByteBuffer.allocateDirect(bs);
			directChunk.set(scratch);
		}

		// reset state
		int rc = XXHash.XXH3_128bits_reset(state);
		if (rc != XXHash.XXH_OK) {
			throw new RuntimeException("XXH3_128bits_reset failed (rc=" + rc + ")");
		}

		size.set(0L);
		header.set(null);

		final byte[] heapBuf = new byte[bs];

		try (OutputStream fos = (cacheContent
				? Files.newOutputStream(tempPath.get(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
				: OutputStream.nullOutputStream())) {

			while (true) {
				int n = content.read(heapBuf);
				if (n < 0) break;
				if (n == 0) continue;

				// capture header on first chunk only (exact bytes read)
				if (header.get() == null) {
					header.set(Arrays.copyOf(heapBuf, n));
				}

				size.set(size.get() + n);

				if (cacheContent) {
					fos.write(heapBuf, 0, n);
				}

				// copy into direct scratch and update hash state
				scratch.clear();
				scratch.put(heapBuf, 0, n);
				scratch.flip();

				rc = XXHash.XXH3_128bits_update(state, scratch);
				if (rc != XXHash.XXH_OK) {
					throw new RuntimeException("XXH3_128bits_update failed (rc=" + rc + ")");
				}
			}

			// digest -> out
			XXHash.XXH3_128bits_digest(state, out);

			// stable 128-bit hex: high||low (32 hex chars)
			long hi = out.high64();
			long lo = out.low64();
			hash.set(toHex128(hi, lo));

			return hash.get();

		} catch (Exception e) {
			throw new RuntimeException("Error computing XXH3_128 hash", e);
		}
	}

	private String hashMessageDigest_streaming(InputStream content, boolean cacheContent) {
		try (OutputStream fos = (cacheContent
				? Files.newOutputStream(tempPath.get(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
				: OutputStream.nullOutputStream())) {

			size.set(0L);

			final int bs = bufferSize.get();
			final byte[] buf = new byte[bs];

			int n = content.read(buf);
			if (n < 0) {
				// empty stream
				header.set(new byte[0]);
				hash.set(toHex(digest.get().digest()));
				return hash.get();
			}

			header.set(Arrays.copyOf(buf, n));

			MessageDigest md = digest.get();
			md.reset();

			while (n >= 0) {
				if (n > 0) {
					size.set(size.get() + n);
					md.update(buf, 0, n);
					if (cacheContent) {
						fos.write(buf, 0, n);
					}
				}
				n = content.read(buf);
			}

			hash.set(toHex(md.digest()));
			return hash.get();

		} catch (Exception e) {
			throw new RuntimeException("Error computing hash", e);
		}
	}

	@Override
	public Optional<String> hash() {
		if (size.get() == 0 || hash.get() == null) {
			return Optional.empty();
		}
		return Optional.of(hash.get());
	}

	@Override
	public Optional<Long> size() {
		if (size.get() < 0) {
			return Optional.empty();
		}
		return Optional.of(size.get());
	}

	@Override
	public Optional<InputStream> getInputStream(boolean deleteOnClose) {
		if (!cacheContent.get() || tempPath.get() == null || !Files.exists(tempPath.get())) {
			return Optional.empty();
		}
		try {
			return Optional.of(deleteOnClose
					? Files.newInputStream(tempPath.get(), StandardOpenOption.DELETE_ON_CLOSE)
					: Files.newInputStream(tempPath.get()));
		} catch (IOException e) {
			throw new RuntimeException("Unable to create an InputStream", e);
		}
	}

	@Override
	public Optional<byte[]> header() {
		byte[] h = header.get();
		if (h == null) {
			return Optional.empty();
		}
		return Optional.of(h);
	}

	public void setBufferSize(int bs) {
		bs = bs < BUFFER_SIZE ? BUFFER_SIZE : bs;
		bufferSize.set(bs);
	}

	public int getBufferSize() {
		return bufferSize.get();
	}

	
	
	// ----------------------------
	// Temp file helpers
	// ----------------------------

	@Override
	public String getAlgorithm() {
		return algorithm.get();
	}

	private void prepareTempFile() {
		try {
			cleanupTempIfPresent();
			tempPath.set(Files.createTempFile(UUID.randomUUID().toString(), "content"));
		} catch (IOException e) {
			throw new RuntimeException("Unable to buffer content", e);
		}
	}

	private void cleanupTempIfPresent() {
		try {
			Path p = tempPath.get();
			if (p != null && Files.exists(p) && Files.isRegularFile(p)) {
				Files.delete(p);
			}
		} catch (IOException ignore) {
			// best-effort cleanup
		}
	}

	// ----------------------------
	// Hex encoding
	// ----------------------------

	private static String toHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			int v = b & 0xFF;
			if (v < 0x10) {
				sb.append('0');
			}
			sb.append(Integer.toHexString(v));
		}
		return sb.toString();
	}

	/**
	 * 128-bit output as 32 lowercase hex chars.
	 * Encoded as high||low (two 64-bit words), fixed width.
	 */
	private static String toHex128(long hi, long lo) {
		return String.format("%016x%016x", hi, lo);
	}
}
