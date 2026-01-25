package com.veritynow.core.store.tools.schema;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.jooq.DSLContext;


public class SchemaManager {
	private static final Pattern CREATE_SCHEMA = Pattern.compile("^\\s*create\\s+schema\\s+", Pattern.CASE_INSENSITIVE);
	
	
	public static void executeScript(DSLContext dsl,   InputStream is) {
		try  {
            
			if (is == null) return;
            // Read the SQL script file into a String
			byte[] bytes = is.readAllBytes();
            String sqlScript = new String(bytes, StandardCharsets.UTF_8);

            // Execute the script
            // Use executeImmediate() for multi-statement scripts if supported by your dialect,
            // or simply fetch() if it's a single statement.
            // For simple DDL/DML, the execute() method works well.
            dsl.execute(sqlScript);
            System.out.println("SQL script executed successfully.");

            

        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	
	//command line
	public static void main(String[] args) throws Throwable {
		if (args.length <= 0) {
			System.out.println("Output directory required");
			System.out.println("Usage: java SchemaGenerator outputDir <schema> <extensions>");
		}
		String schema = "public";
		
		if (args.length > 1) {
			schema = args[1];
		}
		
		String extensions = "ltree";
		if (args.length > 2) {
			extensions = args[2];
		}
		
		generateSchema(args[0], schema, extensions);
	}
	
	
	public static void generateSchema(String destinationPath, String schema, String extensions) throws Throwable {
		

		Path base = Path.of(destinationPath).resolve("schema-baseline");

		Path createPath = withSuffix(base, "-create.sql");
		Path createIfAbsentPath = withSuffix(base, "-create-if-absent.sql");
		Path dropIfExistsPath = withSuffix(base, "-drop-if-exists.sql");

		Files.createDirectories(base.toAbsolutePath().getParent());

		//Prepare schema sql
		String prelude = buildPrelude(schema, extensions);
		
		generateCreateSchema(createPath, prelude);
		generateCreateIfAbsentSchema(createIfAbsentPath, prelude);
		generateDropIfExistsSchema(dropIfExistsPath,  prelude);
		
	}
	
	public static void generateCreateSchema(Path createPath, String prelude) throws Throwable {
		//backup before write
		try {
			List<String> create = SQLCreate.generate();
			backupIfExists(createPath);
			writeScript(createPath, create, prelude);
			System.out.println("SCHEMA_DUMP_WRITTEN_CREATE: " + createPath.toAbsolutePath());
		} catch (Throwable e) {
			System.out.println("create schema failed");
			System.out.println("Terminating generation of createIfAbsent and dropIfExist");
			throw e;
		}
	}
	
	public static void generateCreateIfAbsentSchema(Path createIfAbsentPath, String prelude) {
		//backup before write
		try {
			List<String> createIfAbsent = SQLCreatelfAbsent.generate();
			backupIfExists(createIfAbsentPath);
			writeScript(createIfAbsentPath, createIfAbsent, prelude);
			System.out.println("SCHEMA_DUMP_WRITTEN_CREATE_IF_ABSENT: " + createIfAbsentPath.toAbsolutePath());
		} catch (Throwable e) {
			//don't throw let creation of dropIfExists continue
			System.out.println("createIfAbsent schema failed");
			e.printStackTrace();
		}	
	}
	
	public static void generateDropIfExistsSchema(Path dropIfExistsPath, String prelude) {
		//backup before write
		try {
			List<String> dropIfExists = SQLDropIfExist.generate();
			backupIfExists(dropIfExistsPath);
			writeScript(dropIfExistsPath, dropIfExists, prelude);
			System.out.println("SCHEMA_DUMP_WRITTEN_DROP_IF_EXISTS: " + dropIfExistsPath.toAbsolutePath());
		} catch (Throwable e) {
			System.out.println("dropIfExists schema failed");
			e.printStackTrace();
		}
	}
	
	
	private static void writeScript(Path out, List<String> queries, String prelude) throws Exception {
		try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {

			if (prelude != null && !prelude.isBlank()) {
				w.write(prelude);
				if (!prelude.endsWith("\n")) {
					w.write("\n");
				}
				w.write("\n");
			}

			for (String sql : queries) {

				if (sql == null)
					continue;
				String trimmed = sql.trim();
				if (trimmed.isEmpty())
					continue;

				if (CREATE_SCHEMA.matcher(trimmed).find()) {
					continue;
				}

				if (trimmed.matches("^\\s*create\\s+") && trimmed.contains(" schema ")) {
					System.out.println(trimmed + " ==============================did not catch");
					continue;
				}

				w.write(sql);
				w.write(";\n");
			}
		}
	}
	

	private static String buildPrelude(String schema, String extensionsCsv) {
		StringBuilder sb = new StringBuilder();
		String effectiveSchema = (schema == null || schema.isBlank()) ? "public" : schema.trim();
		sb.append("create schema if not exists \"").append(effectiveSchema).append("\";\n");

		String extCsv = (extensionsCsv == null) ? "" : extensionsCsv.trim();
		if (!extCsv.isEmpty()) {
			for (String ext : extCsv.split(",")) {
				String e = ext == null ? "" : ext.trim();
				if (!e.isEmpty()) {
					sb.append("create extension if not exists ").append(e).append(";\n");
				}
			}
		}
		return sb.toString();
	}

	private static void backupIfExists(Path out) throws Exception {
		if (!Files.exists(out))
			return;

		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
		String filename = dateFormat.format(date) + "-" + out.getFileName();
		Path dest = out.getParent().resolve(Path.of(filename));
		Files.move(out, dest);
	}

	private static Path withSuffix(Path out, String suffix) {
		String name = out.getFileName().toString();
		int dot = name.lastIndexOf('.');
		if (dot <= 0) {
			return out.getParent() != null ? out.getParent().resolve(name + suffix) : Path.of(name + suffix);
		}
		String stem = name.substring(0, dot);
		String ext = name.substring(dot); // includes '.'
		String newName = stem + suffix + ext;
		return out.getParent() != null ? out.getParent().resolve(newName) : Path.of(newName);
	}
}
