package com.veritynow.core.store.db;

import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.DatePart;

public final class DbTime {
    private DbTime() {}

    public static  Field<Long> nowEpochMs() {
    	
        return DSL.floor(
            DSL.extract(DSL.currentTimestamp(), DatePart.EPOCH).mul(DSL.inline(1000))
        ).cast(Long.class);
    }

    public static Field<java.time.OffsetDateTime> nowTimestamptz() {
        return DSL.currentOffsetDateTime();
    }
}