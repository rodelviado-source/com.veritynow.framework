package util;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_INODE;
import static org.jooq.impl.DSL.field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.postgres.extensions.types.Ltree;

import com.veritynow.core.store.versionstore.repo.PathKeyCodec;


public class DBUtil {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void ensureProjectionReady(DSLContext dsl) {
        // Root is store-owned and identified by scope_key = PathKeyCodec.ROOT_LABEL.
        int cnt = dsl.fetchCount(
            VN_INODE,
            VN_INODE.SCOPE_KEY.eq(Ltree.ltree(PathKeyCodec.ROOT_LABEL))
        );

        if (cnt == 0) {
            throw new IllegalStateException(
                "Root inode missing for scope_key=" + PathKeyCodec.ROOT_LABEL
            );
        }

        // Fetch canonical text form (scope_key::text) for debug parity with JDBC version
        String rootScopeKey = dsl
            .select(field("{0}::text", String.class, VN_INODE.SCOPE_KEY))
            .from(VN_INODE)
            .where(VN_INODE.SCOPE_KEY.eq(Ltree.ltree(PathKeyCodec.ROOT_LABEL)))
            .fetchOne(0, String.class);

        if (rootScopeKey == null) {
            throw new IllegalStateException(
                "vn_inode.scope_key is NULL for root scope_key=" + PathKeyCodec.ROOT_LABEL
            );
        }

        // 2) List columns of vn_inode
        try {
            Result<Record> cols = dsl.fetch("""
                select column_name, data_type, is_nullable
                from information_schema.columns
                where table_name = 'vn_inode'
                order by ordinal_position
                """);

            for (Record r : cols) {
                LOGGER.info(
                    "vn_inode column: name={}, type={}, nullable={}",
                    r.get("column_name", String.class),
                    r.get("data_type", String.class),
                    r.get("is_nullable", String.class)
                );
            }
        } catch (Exception e) {
            LOGGER.error("vn_inode column introspection failed", e);
        }

        // 3) Explicit check for an 'id' column
        try {
            
			Record idCount = dsl.fetchOne("""
                select count(*)
                from information_schema.columns
                where table_name = 'vn_inode'
                  and lower(column_name) = 'id'
                """, Integer.class);

            LOGGER.info("vn_inode has 'id' column count = {}", idCount.getValue(0));
        } catch (Exception e) {
            LOGGER.error("vn_inode id-column check failed", e);
        }

        // 4) Show all schemas that contain vn_inode
        try {
            Result<Record> schemas = dsl.fetch("""
                select table_schema, table_name
                from information_schema.tables
                where table_name = 'vn_inode'
                """);

            for (Record r : schemas) {
                LOGGER.info("vn_inode found in schema: {}", r.get("table_schema", String.class));
            }
        } catch (Exception e) {
            LOGGER.error("vn_inode schema scan failed", e);
        }

        // 5) Log current schema & search path
        try {
            Record schema = dsl.fetchOne("select current_schema()", String.class);
            Record searchPath = dsl.fetchOne("show search_path", String.class);
            LOGGER.info("current_schema={}, search_path={}", schema.getValue(0), searchPath.getValue(0));
        } catch (Exception e) {
            LOGGER.error("schema/search_path lookup failed", e);
        }

        LOGGER.info("=== DIAGNOSE vn_inode END ===");
    }
}
