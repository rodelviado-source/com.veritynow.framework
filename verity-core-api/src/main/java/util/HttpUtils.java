package util;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

public class HttpUtils {
	
	public static UriComponentsBuilder newUriBuilder() {
		return UriComponentsBuilder.newInstance();
	}
	
	public static String extractFilenameFromHeaderContentDisposition(HttpServletRequest request ) {
		String cdHeader = request.getHeader(HttpHeaders.CONTENT_DISPOSITION);
		if (cdHeader != null) {
		    ContentDisposition cd = ContentDisposition.parse(cdHeader);
		    return cd.getFilename();
		   
		}
		return null;
		
	}

}
