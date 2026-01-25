package com.veritynow;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class Application {
  public static void main(String[] args) {
    System.setProperty("org.jooq.no-logo", "true");
    SpringApplication.run(Application.class, args);
  }
}
