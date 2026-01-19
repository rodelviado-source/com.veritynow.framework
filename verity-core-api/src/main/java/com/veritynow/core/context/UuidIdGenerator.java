package com.veritynow.core.context;

import java.util.UUID;

public final class UuidIdGenerator implements IdGenerator {
    @Override
    public String newCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
