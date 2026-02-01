package com.veritynow.api;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public record APITransaction(
		List<Transaction> transactions,
		Map<String, InputStream> blobs
) {}
