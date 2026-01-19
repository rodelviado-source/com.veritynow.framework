package util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

import com.veritynow.core.store.jpa.PathKeyCodec;

public class DBUtil {
private static final Logger LOGGER = LogManager.getLogger();

    public static void ensureProjectionReady(JdbcTemplate jdbc) {
        // Root is store-owned and identified by scope_key = PathKeyCodec.ROOT_LABEL.
        Integer cnt = jdbc.queryForObject(
            "select count(*) from vn_inode where scope_key = cast(? as ltree)",
            Integer.class,
            PathKeyCodec.ROOT_LABEL
        );

        if (cnt == null || cnt == 0) {
            throw new IllegalStateException(
                "Root inode missing for scope_key=" + PathKeyCodec.ROOT_LABEL
            );
        }

        String rootScopeKey = jdbc.queryForObject(
            "select scope_key::text from vn_inode where scope_key = cast(? as ltree)",
            String.class,
            PathKeyCodec.ROOT_LABEL
        );

        if (rootScopeKey == null) {
            throw new IllegalStateException(
                "vn_inode.scope_key is NULL for root scope_key=" + PathKeyCodec.ROOT_LABEL
            );
        }
            
	
	    // 2) List columns of vn_inode
	    try {
	        jdbc.query("""
	            select column_name, data_type, is_nullable
	            from information_schema.columns
	            where table_name = 'vn_inode'
	            order by ordinal_position
	            """,
	            rs -> {
	            	LOGGER.info(
	                    "vn_inode column: name={}, type={}, nullable={}",
	                    rs.getString("column_name"),
	                    rs.getString("data_type"),
	                    rs.getString("is_nullable")
	                );
	            }
	        );
	    } catch (Exception e) {
	    	LOGGER.error("vn_inode column introspection failed", e);
	    }

	    // 3) Explicit check for an 'id' column
	    try {
	        Integer idCount = jdbc.queryForObject("""
	            select count(*)
	            from information_schema.columns
	            where table_name = 'vn_inode'
	              and lower(column_name) = 'id'
	            """, Integer.class);

	        LOGGER.info("vn_inode has 'id' column count = {}", idCount);
	    } catch (Exception e) {
	    	LOGGER.error("vn_inode id-column check failed", e);
	    }

	    // 4) Show all schemas that contain vn_inode
	    try {
	        jdbc.query("""
	            select table_schema, table_name
	            from information_schema.tables
	            where table_name = 'vn_inode'
	            """,
	            rs -> {
	            	LOGGER.info(
	                    "vn_inode found in schema: {}",
	                    rs.getString("table_schema")
	                );
	            }
	        );
	    } catch (Exception e) {
	    	LOGGER.error("vn_inode schema scan failed", e);
	    }

	    // 5) Log current schema & search path
	    try {
	        String schema = jdbc.queryForObject("select current_schema()", String.class);
	        String searchPath = jdbc.queryForObject("show search_path", String.class);
	        LOGGER.info("current_schema={}, search_path={}", schema, searchPath);
	    } catch (Exception e) {
	    	LOGGER.error("schema/search_path lookup failed", e);
	    }

	    LOGGER.info("=== DIAGNOSE vn_inode END ===");
	}

}
