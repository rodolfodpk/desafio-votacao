package com.rdpk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cpf.validation")
public class CpfValidationConfig {
    
    private boolean lenient = true; // Default to lenient mode for testing
    
    public boolean isLenient() {
        return lenient;
    }
    
    public void setLenient(boolean lenient) {
        this.lenient = lenient;
    }
}

