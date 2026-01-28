package util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FSUtil {
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static void safeWrite(Path dir, String name, byte[] bytes) throws IOException {
		String uuid = UUID.randomUUID().toString();
		
		Path tmp = dir.resolve(name + "." + uuid);
        Path out = dir.resolve(name);

        Files.write(tmp, bytes, StandardOpenOption.CREATE_NEW);
        Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        
	}
	
	public static void safeWrite(Path dir, String name, InputStream is) throws IOException {
		String uuid = UUID.randomUUID().toString();
		
		Path tmp = dir.resolve(name + "." + uuid);
        Path out = dir.resolve(name);
        
        try (FileOutputStream fos = new FileOutputStream(tmp.toFile())) {
	        byte[] b = is.readNBytes(1024);
	        while (b.length > 0) {
	        	fos.write(b);
	        	b = is.readNBytes(1024);
	        } 
        }
        try {
            Files.move(tmp, out, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // Fallback must be explicit overwrite — bytes are non-authoritative until meta exists
            Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING);
        }
	}
	
	public static void safeWrite(Path dir, String name, String s) throws IOException {
		safeWrite(dir,name,s.getBytes(StandardCharsets.UTF_8));
	}
	
	public static void safeWrite(Path path, byte[] b) throws IOException {
		safeWrite(path.getParent(), path.getFileName().toString(),b);
	}

	public static void safeWrite(Path path, InputStream is) throws IOException {
		safeWrite(path.getParent(), path.getFileName().toString(),is);
	}
	
	public static void safeWrite(Path path, String s) throws IOException {
		safeWrite(path.getParent() ,path.getFileName().toString(),s.getBytes(StandardCharsets.UTF_8));
	}
	
	public static void deleteRecursively(Path root) {
		LOGGER.info("Deleting dataDir {}", root);
		try {
			if (root == null || !Files.exists(root))
				return;
			Files.walk(root).sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.deleteIfExists(p);
				} catch (IOException ignored) {
				}
			});
		} catch (IOException ignored) {
		}

	}
	
}
