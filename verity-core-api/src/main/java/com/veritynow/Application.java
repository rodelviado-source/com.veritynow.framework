package com.veritynow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class Application {
  private static final Logger LOGGER = LogManager.getLogger();	
  public static void main(String[] args) {
	  
	LOGGER.info(
			
"\n _   __        _ __         _  __" +          
"\n| | / /__ ____(_) /___ __  / |/ /__ _    __" +
"\n| |/ / -_) __/ / __/ // / /    / _ \\ |/|/ /" +
"\n|___/\\__/_/ /_/\\__/\\_, / /_/|_/\\___/__,__/" + 
"\n                  /___/\n" +                    
"Thank you for using VerityNow Framework version 1.0.0"			
			
);
	
	
    SpringApplication.run(Application.class, args);
  }
}
