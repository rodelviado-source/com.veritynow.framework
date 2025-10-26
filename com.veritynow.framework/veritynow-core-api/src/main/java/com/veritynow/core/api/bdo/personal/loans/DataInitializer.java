package com.veritynow.core.api.bdo.personal.loans;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.transaction.annotation.Transactional;

// imports...

@Configuration
public class DataInitializer {
	@Value("${app.seed.skip:true}")
	private boolean skipSeed;

	// repos + ctor ...

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void initData() throws Exception {
		if (skipSeed)
			return; // 👈 respect config

		// ... existing seeding code (users, images, records)
	}

	// ...
}
