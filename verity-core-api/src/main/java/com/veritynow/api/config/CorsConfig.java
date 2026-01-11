package com.veritynow.api.config;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Configuration
public class CorsConfig {

	private static final Logger LOGGER = LogManager.getLogger();
	private final CorsProperties props;

	public CorsConfig(CorsProperties props) {
		this.props = props;
	}

	@Bean
	public CorsFilter corsFilter() {
		CorsConfiguration config = new CorsConfiguration();

		config.setMaxAge(props.getMaxAge());
		config.setAllowCredentials(props.isAllowCredentials());
		config.setAllowPrivateNetwork(true);
		config.setAllowedOriginPatterns(props.getAllowedOrigins());
		config.setAllowedMethods(props.getAllowedMethods());
		config.setAllowedHeaders(props.getAllowedHeaders());

		ObjectMapper om = new ObjectMapper();
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		props.getPaths().forEach(p -> {
			source.registerCorsConfiguration(p, config);

			try {

				om.configure(SerializationFeature.INDENT_OUTPUT, true);

				LOGGER.info("\nRegistered: Path({}) -> {}", p, om.writeValueAsString(config));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		return new CorsFilter(source);
	}
}
