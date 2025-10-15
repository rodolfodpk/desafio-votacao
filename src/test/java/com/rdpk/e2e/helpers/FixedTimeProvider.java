package com.rdpk.e2e.helpers;

import com.rdpk.infrastructure.time.TimeProvider;
import java.time.LocalDateTime;

public class FixedTimeProvider implements TimeProvider {
    private LocalDateTime fixedTime;
    
    public FixedTimeProvider(LocalDateTime fixedTime) {
        this.fixedTime = fixedTime;
    }
    
    @Override
    public LocalDateTime now() {
        return fixedTime;
    }
    
    public void setTime(LocalDateTime newTime) {
        this.fixedTime = newTime;
    }
    
    public void advance(long minutes) {
        this.fixedTime = this.fixedTime.plusMinutes(minutes);
    }
}
