package util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

public class HttpUtils {
	
	public static UriComponentsBuilder newUriBuilder() {
		return UriComponentsBuilder.newInstance();
	}
	
	public static Optional<String> extractFilenameFromHeaderContentDisposition(HttpServletRequest request ) {
		String CD = request.getHeader(HttpHeaders.CONTENT_DISPOSITION);
		try {
		if (CD != null) {
		    ContentDisposition cd = ContentDisposition.parse(CD);
		    String fn = cd.getFilename();
		    if (StringUtils.isNotEmpty(fn));
		    	return Optional.of(fn);
		}
		} catch (Exception e) {
			//try regex
			String fn = getFilenameFromContentDisposition(CD);
			if (StringUtils.isNotEmpty(fn)) {
	    		return Optional.of(fn);
			}	
	    		
			throw new IllegalArgumentException("Invalid header field parameter format (as defined in RFC 5987 or ISO-8859-1)",e);
		}
		return Optional.empty();
		
	}
	
	//
	private static String getFilenameFromContentDisposition(String contentDisposition) {
	    if (contentDisposition == null) {
	        return null;
	    }

	    // Pattern to find 'filename=' or 'filename*=' parameter, handling optional quotes
	    Pattern regex = Pattern.compile("filename\\*?=['\"]?([^;\"\\n]+?)['\"]?(?:;|$)", Pattern.CASE_INSENSITIVE);
	    Matcher matcher = regex.matcher(contentDisposition);

	    if (matcher.find()) {
	        // Filename is in the first capturing group
	        String encodedFilename = matcher.group(1);
	        
	        // Handle RFC 5987 encoded filenames (e.g., filename*=UTF-8''...)
	        if (contentDisposition.toLowerCase().contains("filename*=utf-8''")) {
	            // URL decode the value
	            return URLDecoder.decode(encodedFilename, StandardCharsets.UTF_8).replace("+", "%20");
	        } else {
	            // Default to ISO-8859-1 for non-encoded filenames (HTTP default)
	            // You might need to handle quoted characters if present
	            return encodedFilename.replace("\"", "").trim();
	        }
	    }

	    return null;
	}

}
