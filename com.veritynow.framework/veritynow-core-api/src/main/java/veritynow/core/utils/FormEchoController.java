package veritynow.core.utils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;




@RestController
@RequestMapping("/api/forms")
public class FormEchoController {

    @PostMapping(
            path = "/echo",
            consumes = { MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE },
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> echoAll(
            HttpServletRequest request,
            @RequestParam Map<String, String> flatParams,
            @RequestParam MultiValueMap<String, String> multiParams,
            @RequestPart(required = false) Map<String, MultipartFile> files
    ) {
        Map<String, Object> out = new LinkedHashMap<>();

        // Include query string parameters as well
        Map<String, List<String>> params = new LinkedHashMap<>();
        if (multiParams != null) {
            for (Map.Entry<String, List<String>> e : multiParams.entrySet()) {
                params.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
        }

        // Also include any parameters discovered via request.getParameterMap()
        request.getParameterMap().forEach((k, v) -> {
            params.computeIfAbsent(k, _k -> new ArrayList<>());
            params.get(k).addAll(Arrays.asList(v));
        });

        // Deduplicate values while preserving order
        params.replaceAll((k, v) -> v.stream().distinct().collect(Collectors.toList()));
        
        
        ObjectMapper om = new ObjectMapper();
        
        try {
        	String prettyPrint = om.writerWithDefaultPrettyPrinter().writeValueAsString(params);
        	prettyPrint = URLDecoder.decode(prettyPrint, StandardCharsets.UTF_8);
        	System.out.println(prettyPrint);
			out.put("fields", prettyPrint);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
        
        

        // Files (if multipart was used)
        if (files != null && !files.isEmpty()) {
            Map<String, Object> fileInfo = new LinkedHashMap<>();
            files.forEach((name, file) -> {
            	System.out.println(file.getOriginalFilename());
                Map<String, Object> meta = new LinkedHashMap<>();
                System.out.println(name + " -- " + file.getOriginalFilename());
                meta.put("originalFilename", file.getOriginalFilename());
                meta.put("size", file.getSize());
                meta.put("contentType", file.getContentType());
                fileInfo.put(name, meta);
            });
            out.put("files", fileInfo);
        } else {
            out.put("files", Collections.emptyMap());
        }

        // Misc request info
        out.put("method", request.getMethod());
        out.put("contentType", request.getContentType());
        out.put("path", request.getRequestURI());

        
        return out;
        
        
    }

    @GetMapping("/echo/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok");
    }
    
}
