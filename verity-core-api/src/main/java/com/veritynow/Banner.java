package com.veritynow;

import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import com.veritynow.util.JSON;

@Configuration
public class Banner {
	private static final Logger LOGGER = LogManager.getLogger();
	private static String banner = 
  """ 
  _   __        _ __         _  __          
 | | / /__ ____(_) /___ __  / |/ /__ _    __
 | |/ / -_) __/ / __/ // / /    / _ \\ |/|/ /
 |___/\\__/_/ /_/\\__/\\_, / /_/|_/\\___/__,__/ 
                   /___/                    
 """;

	@EventListener(ApplicationReadyEvent.class)
	public void printBanner() {
		String version = "0.0.0";
		String jsonProps = "{}";
		try {  
			InputStream pp = getClass().getResourceAsStream("/project.properties");
			if (pp != null) {
				 Properties props = new Properties();
				 props.load(pp);
				 
				 jsonProps =  JSON.MAPPER_PRETTY.writeValueAsString(props);
				 String v = props.getProperty("project.version");
				 if (v != null) {
					 version = v;
				 }
			} else {
				System.out.println("Not found");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		  
		LOGGER.info("\n{}\n{}Thank you for using VerityNow Framework version {}",jsonProps, banner,  version);
	}
	
}
