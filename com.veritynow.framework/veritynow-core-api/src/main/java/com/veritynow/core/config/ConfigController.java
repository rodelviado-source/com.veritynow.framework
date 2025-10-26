package com.veritynow.core.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class ConfigController {


        @Value("${app.ui.forms.url}")
        private String uiFormUrl;

        @GetMapping
        public Map<String, String> getConfig() {
            Map<String, String> config = new HashMap<>();
            config.put("uiFormUrl", uiFormUrl);
            return config;
        }
        
        
        @GetMapping("/ping")
        public Map<String, String> ping() {
            return Map.of("status", "ok");
        }
        
}


