package com.veritynow.v2.store.meta;

public record BlobMeta(
        String hash,
        String name,
        String mimeType,
        long   size
) {}
