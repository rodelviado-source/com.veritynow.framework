package util;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

public class DBUtil {
private static final Logger LOGGER = LogManager.getLogger();
    
    
    private final static AtomicBoolean projectionReady = new AtomicBoolean(false);

    private static final String ENSURE_SCOPE_ROOT =
        """
        INSERT INTO vn_scope_index(inode_id, path_text, scope_key)
        SELECT min(id), '/', vn_path_to_scope_key('/')
        FROM vn_inode
        WHERE EXISTS (SELECT 1 FROM vn_inode)
        ON CONFLICT (inode_id) DO NOTHING
        """;
    
    public static void ensureProjectionReady(JdbcTemplate jdbc) {
        // 1) Ensure vn_root exists and has exactly one row
        Long rootInodeId = jdbc.queryForObject(
            "select inode_id from vn_root where singleton = TRUE",
            Long.class
        );

        if (rootInodeId == null) {
            throw new IllegalStateException(
                "vn_root missing singleton row; locking support not initialized correctly"
            );
        }

        // 2) Ensure vn_scope_index has the root projection row
        Integer scopeCount = jdbc.queryForObject(
            """
            select count(*)
            from vn_scope_index
            where inode_id = ?
            """,
            Integer.class,
            rootInodeId
        );

        if (scopeCount == null || scopeCount == 0) {
            throw new IllegalStateException(
                "vn_scope_index missing root inode projection for inode_id=" + rootInodeId
            );
        }
    }


	
	public static void diagnoseVnInode(JdbcTemplate jdbc) {
	    

	    LOGGER.info("=== DIAGNOSE vn_inode BEGIN ===");

	    // 1) What object is vn_inode (table / view / schema)?
	    try {
	        jdbc.query("""
	            select
	              c.oid::regclass as regclass,
	              n.nspname as schema,
	              c.relname,
	              c.relkind
	            from pg_class c
	            join pg_namespace n on n.oid = c.relnamespace
	            where c.oid = 'vn_inode'::regclass
	            """,
	            rs -> {
	            	LOGGER.info(
	                    "vn_inode object: regclass={}, schema={}, relname={}, relkind={}",
	                    rs.getString("regclass"),
	                    rs.getString("schema"),
	                    rs.getString("relname"),
	                    rs.getString("relkind")
	                );
	            }
	        );
	    } catch (Exception e) {
	    	LOGGER.error("vn_inode object lookup failed", e);
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
