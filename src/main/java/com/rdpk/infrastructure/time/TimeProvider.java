package com.rdpk.infrastructure.time;

import java.time.LocalDateTime;

public interface TimeProvider {
    LocalDateTime now();
}
