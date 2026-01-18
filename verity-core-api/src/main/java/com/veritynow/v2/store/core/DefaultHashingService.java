package com.veritynow.v2.store.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import com.veritynow.v2.store.HashingService;

/**
 * HashingService implementation using SHA-256 and lowercase hex encoding.
 *
 * Hash format example: "e3b0c44298fc1c149afbf4c8996fb924..."
 * 
 * Thread safe implementation
 */
public class DefaultHashingService implements HashingService {

	public static int BUFFER_SIZE = 1024;
	
	private static final Logger LOGGER = LogManager.getLogger();
	
	private final ThreadLocal<MessageDigest> digest;
	private final ThreadLocal<Long> size = ThreadLocal.withInitial(() -> {
		return -1L;
	});
	private final ThreadLocal<String> hash = ThreadLocal.withInitial(() -> {
		return null;
	});
	private final ThreadLocal<byte[]> header = ThreadLocal.withInitial(() -> {
		return null;
	});
	private final ThreadLocal<Path> tempPath = new ThreadLocal<Path>();
	private final ThreadLocal<Boolean> cacheContent =  ThreadLocal.withInitial(() -> {
		return false;
	});
	private final ThreadLocal<Integer> bufferSize =  ThreadLocal.withInitial(() -> {
		return BUFFER_SIZE;
	});
	
	public DefaultHashingService(@Value("${verity.store.hash.algo:SHA-1}") String algo)
			throws NoSuchAlgorithmException {
		this.digest = ThreadLocal.withInitial(() -> {
			try {
				return MessageDigest.getInstance(algo);
			} catch (NoSuchAlgorithmException e) {
				return null;
			}
		});

		if (this.digest == null) {
			throw new NoSuchAlgorithmException(algo);
		}
		LOGGER.info("\n\tDefault hashing service using {} algorithm started ", algo);
		
	}

	@Override
	public String hash(Path contentPath) {
		Objects.requireNonNull(contentPath, "content");
		try { 
			if (Files.isRegularFile(contentPath)) {
				try (InputStream is = Files.newInputStream(contentPath)) {
					return hash(is, false);
				} 	
			}
			throw new RuntimeException("Path is not a regular file " + contentPath);
		} catch (Exception e) {
			throw new RuntimeException("Error computing hash " + contentPath, e);
		}
	}




	@Override
	public String hash(InputStream content, boolean cacheContent) {
		Objects.requireNonNull(content, "content");
		this.cacheContent.set(cacheContent);
		
		if (cacheContent) {
			try {
				if (tempPath.get() != null && Files.exists(tempPath.get()) && Files.isRegularFile(tempPath.get())) {
					Files.delete(tempPath.get());
				}
				tempPath.set(Files.createTempFile(UUID.randomUUID().toString(), "content"));
			} catch (IOException e) {
				throw new RuntimeException("Unable to buffer content", e);
			}
		}

		
		try (OutputStream fos = (cacheContent ? Files.newOutputStream(tempPath.get(), StandardOpenOption.WRITE)
				: OutputStream.nullOutputStream())) {
			
			size.set(0L);
			byte[] bytes = content.readNBytes(bufferSize.get());
			if (cacheContent)
				fos.write(bytes);
			header.set(bytes);

			MessageDigest md = digest.get();
			md.reset();
			while (bytes.length > 0) {
				size.set(size.get() + bytes.length);
				md.update(bytes);
				bytes = content.readNBytes(bufferSize.get());
				if (cacheContent)
					fos.write(bytes);
			}
			hash.set(toHex(md.digest()));
			return hash.get();
		} catch (Exception e) {
			throw new RuntimeException("Error computing hash ", e);
		}
	}

	@Override
	public Optional<String> hash() {
		if (size.get() == 0 || hash.get() == null)
			return Optional.empty();
		return Optional.of(hash.get());
	}

	@Override
	public Optional<Long> size() {
		if (size.get() < 0)
			return Optional.empty();
		return Optional.of(size.get());
	}

	@Override
	public Optional<InputStream> getInputStream(boolean deleteOnClose) {
		if (!cacheContent.get() || !Files.exists(tempPath.get()))
			return Optional.empty();
		try {
			return Optional.of(
					deleteOnClose ? 
					  Files.newInputStream(tempPath.get(), StandardOpenOption.DELETE_ON_CLOSE)
					: Files.newInputStream(tempPath.get()));
		} catch (IOException e) {
			throw new RuntimeException("Unable to create an InputStream", e);
		}
	}

	@Override
	public Optional<byte[]> header() {
		if (header == null)
			return Optional.empty();
		return Optional.of(header.get());
	}

	
	
	public void setBufferSize(int bs) {
		bs = bs < BUFFER_SIZE ? BUFFER_SIZE : bs;
		bufferSize.set(bs);
	}
	
	public int getBufferSize() {
		return bufferSize.get();
	}

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
}