package util;

import static org.jooq.impl.DSL.currentOffsetDateTime;
import static org.jooq.impl.DSL.currentTimestamp;
import static org.jooq.impl.DSL.epoch;
import static org.jooq.impl.DSL.floor;
import static org.jooq.impl.DSL.inline;

import org.jooq.Field;

public final class DBTime {
    private DBTime() {}

    public static  Field<Long> nowEpochMs() {
    	return floor(
   			epoch(currentTimestamp()).mul(inline(1000))	
        ).cast(Long.class);
    }

    public static Field<java.time.OffsetDateTime> nowTimestamptz() {
        return currentOffsetDateTime();
    }
}