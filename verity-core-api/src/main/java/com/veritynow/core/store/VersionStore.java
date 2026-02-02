package com.veritynow.core.store;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Version-aware extension of {@link Store} that exposes per-path version history and
 * path-scoped enumeration APIs.
 *
 * <p>
 * This interface keeps the same core semantics as {@link Store} for:
 * </p>
 * <ul>
 *   <li><b>Existence</b>: a path is considered present iff a latest version (HEAD) is resolvable
 *       (see {@link Store#exists(Object)} and {@link #getLatestVersion(String)}).</li>
 *   <li><b>Restore</b>: restore moves HEAD to a prior version regardless of deletion state
 *       (see {@link Store#restore(Object)}).</li>
 *   <li><b>Non-transactional contract</b>: this interface does not prescribe transactional semantics;
 *       atomicity across multiple calls is implementation-defined (see {@link Store}).</li>
 * </ul>
 *
 * <h3>Key model</h3>
 * <ul>
 *   <li>{@code PK} identifies a concrete stored object instance (often a composite such as
 *       {@code (path, versionId)} or {@code (path, contentHash)}).</li>
 *   <li>{@code VERSIONMETA} describes individual version entries for a logical {@code path}
 *       (e.g., hash, timestamp, operation, principal, correlationId, etc.).</li>
 * </ul>
 *
 * <h3>Listing semantics</h3>
 * <ul>
 *   <li>{@link #getChildrenLatestVersion(String)} returns latest-version entries directly under {@code path} (non-recursive).</li>
 *   <li>{@link #getChildrenPath(String)} returns direct child segments under {@code path} (non-recursive).</li>
 * </ul>
 *
 * <p>
 * The exact interpretation of "under this path" (delimiter rules, normalization, trailing slash handling)
 * is implementation-defined but must be deterministic within an implementation.
 * </p>
 *
 * @param <PK>          key type identifying a concrete stored object (often includes path + version identity)
 * @param <BLOBMETA>    metadata type returned by {@link Store} CRUD operations
 * @param <VERSIONMETA> metadata type describing individual versions for a path
 */
public interface VersionStore<PK, BLOBMETA, VERSIONMETA> extends Store<PK, BLOBMETA> {

    /**
     * Resolve the latest version metadata for the given logical path.
     *
     * <p>
     * Returns empty if no latest version is resolvable for the path (e.g., never created, removed, or
     * otherwise not currently addressable according to store rules).
     * </p>
     *
     * @param path logical path
     * @return latest version metadata if resolvable
     * @throws IOException on I/O failure
     */
    Optional<VERSIONMETA> getLatestVersion(String path) throws IOException;

    /**
     * Return all version metadata entries for the given logical path.
     *
     * <p>
     * The returned list must be deterministic. Implementations should document ordering
     * (e.g., chronological ascending/descending, or by internal sequence).
     * </p>
     *
     * @param path logical path
     * @return all version metadata entries for the path (possibly empty)
     * @throws IOException on I/O failure
     */
    List<VERSIONMETA> getAllVersions(String path) throws IOException;

    /**
     * List the latest versions of blobs directly under the given logical path (non-recursive).
     *
     * <p>
     * This behaves like a "directory listing" where each returned entry corresponds to a direct child
     * path under {@code path}, and the metadata represents the latest version for that child.
     * </p>
     *
     * @param path logical parent path
     * @return latest version metadata for direct children (possibly empty)
     * @throws IOException on I/O failure
     */
    List<VERSIONMETA> getChildrenLatestVersion(String path) throws IOException;

    /**
     * List the direct child names/segments under the given logical path (non-recursive).
     *
     * <p>
     * This is the "names only" companion to {@link #getChildrenLatestVersion(String)}. The returned list should be
     * deterministic and contain each child exactly once.
     * </p>
     *
     * @param path logical parent path
     * @return direct child names/segments (possibly empty)
     * @throws IOException on I/O failure
     */
    List<String> getChildrenPath(String path) throws IOException;

    /**
     * Retrieve the binary content for a concrete key {@code PK}.
     *
     * <p>
     * This is a key-based content fetch (as opposed to {@link Store#read(Object)} which may be
     * path/latest-version based depending on {@code PK}). If the key is not resolvable, returns empty.
     * </p>
     *
     * <p>
     *
     * @param key concrete key identifying a specific stored object instance
     * @return content stream if resolvable
     * @throws IOException on I/O failure
     */
    Optional<InputStream> getContent(PK key) throws IOException;
}
