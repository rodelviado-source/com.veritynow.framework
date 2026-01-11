package util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public class FSUtil {
	
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
        
        Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
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
	
	
	
}
