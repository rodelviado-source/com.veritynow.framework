package com.veritynow.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class DevConfig {

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		
	System.err.println("=======================GOT CALLED=======================");	
	  return new WebMvcConfigurer() {
	    @Override
	    public void addCorsMappings(CorsRegistry registry) {
	      registry.addMapping("/api/**")
	        .allowedOrigins("http://localhost:5173", "http://localhost:8080")
	        .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
	        .allowedHeaders("*");
	    }
	  };
	}

	
	
}
