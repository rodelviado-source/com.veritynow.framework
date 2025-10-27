package com.veritynow.core.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig  implements WebMvcConfigurer {
	

	private static final String CORS_PATHS = "${cors.paths}";
	private static final String CORS_ALLOWED_ORIGINS = "${cors.allowed.origins}";
	private static final String CORS_ALLOWED_METHODS = "${cors.allowed.methods}";
	private static final String CORS_ALLOWED_HEADERS = "${cors.allowed.headers}";
	private static final String CORS_ALLOWED_CREDENTIALS = "${cors.allowed.credentials}";
	private static final String CORS_MAX_AGE = "${cors.max.age}";
	
	private static final String APP_UI_FORM_URL = "${app.ui.forms.url}";
	
	@Value(CORS_PATHS)
    private String[] corsPaths;
	
	@Value(CORS_ALLOWED_ORIGINS)
    private String[] corsAllowedOrigins;

    @Value(CORS_ALLOWED_METHODS)
    private String[] corsAllowedMethods;

    @Value(CORS_ALLOWED_HEADERS)
    private String[] corsAllowedHeaders;

    @Value(CORS_ALLOWED_CREDENTIALS)
    private boolean corsAllowCredentials;

    @Value(CORS_MAX_AGE)
    private long corsMaxAge;
    
    @Value(APP_UI_FORM_URL)
    private String uiFormUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
    	
    	Arrays.asList(corsPaths).forEach(path -> { 
    		
        registry.addMapping(path) // Apply to all paths
                .allowedOrigins(corsAllowedOrigins)
                .allowedMethods(corsAllowedMethods)
                .allowedHeaders(corsAllowedHeaders)
                .allowCredentials(corsAllowCredentials)
                .maxAge(corsMaxAge);
        		printValues();
        		
    	});
    	
    	
    }
    
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
    	

    }
	
    private void printValues() {
    	
    	System.out.println(CORS_PATHS + Arrays.asList(corsPaths));
    	System.out.println(CORS_ALLOWED_ORIGINS + " = " + Arrays.asList(corsAllowedOrigins));
    	System.out.println(CORS_ALLOWED_HEADERS  +" = " +  Arrays.asList(corsAllowedHeaders));
    	System.out.println(CORS_ALLOWED_METHODS + " = " + Arrays.asList(corsAllowedMethods));
    	System.out.println(CORS_ALLOWED_CREDENTIALS + " = " + corsAllowCredentials);
    	System.out.println(CORS_MAX_AGE + " = " + corsMaxAge);
    	System.out.println(APP_UI_FORM_URL + " = " + uiFormUrl);
    	
    }
    
    
      
  }
  
  
  

