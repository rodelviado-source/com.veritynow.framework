package com.veritynow.core.store.spring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.veritynow.core.store.tools.schema.SchemaManager;

@Component
@ConditionalOnProperty(name = "verity.schema.dump.enabled", havingValue = "true")
public class SchemaDumpOnReady {


	@Value("${verity.schema.dump.schema:public}")
	private String schema;

	/**
	 * Comma-separated list of Postgres extensions to include in the generated SQL
	 * output. Default includes ltree because the store relies on it.
	 */
	@Value("${verity.schema.dump.extensions:ltree}")
	private String extensions;

	@Value("${verity.schema.dump.path:ddl}")
	private String destinationPath;

	public SchemaDumpOnReady() {
		
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onReady() throws Exception {
		try {
			SchemaManager.generateSchema(destinationPath, schema, extensions);
			System.out.println("To turn off schema dump, set verity.schema.dump.enabled=false in application.properties");
		} catch (Throwable e) {
			System.out.println("schema generation failed");
			e.printStackTrace();
		}
	}
}
