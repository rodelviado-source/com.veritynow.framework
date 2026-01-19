package com.veritynow.core.store.meta;

public record BlobMeta(
        String hash,
        String name,
        String mimeType,
        long   size
) {}
