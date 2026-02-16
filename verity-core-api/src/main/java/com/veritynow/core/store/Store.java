package com.veritynow.core.store;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Generic content-addressable store abstraction for versioned objects identified by {@code KEY}
 * and described by metadata {@code META}.
 *
 * <p>
 * The {@code Store} interface models a logical key → versioned blob mapping where:
 * </p>
 * <ul>
 *   <li>{@code KEY} identifies a logical path or address (not a physical storage location)</li>
 *   <li>{@code META} represents immutable metadata describing a stored version
 *       (e.g. hash, size, timestamp, operation, principal, etc.)</li>
 *   <li>Binary content is supplied and consumed as {@link InputStream}s</li>
 * </ul>
 *
 * <h3>Existence semantics</h3>
 * <p>
 * {@code exists(KEY)} indicates whether the store currently has a resolvable
 * latest version (HEAD) for the given key. It does not report whether the key
 * ever existed historically.
 * </p>
 *
 * <p>
 * A key with no resolvable latest version (e.g. never created, or permanently
 * removed according to store rules) is considered non-existent.
 * </p>
 *
 * <h3>Creation and identity</h3>
 * <ul>
 *   <li>The store is responsible for generating authoritative version identifiers
 *       unless explicitly provided via an {@code id} overload.</li>
 *   <li>Methods returning {@link Optional} return {@link Optional#empty()} if the
 *       operation results in no state change (e.g. creating an already-existing key
 *       when the implementation chooses to no-op).</li>
 * </ul>
 *
  * <h3>Deletion and restore model</h3>
 * <ul>
 *   <li>{@code delete} records a deletion operation for the key.</li>
 *   <li>{@code undelete} reverses a delete when the current state is deleted.</li>
 *   <li>{@code restore} moves the current state (HEAD) to a prior version,
 *       regardless of whether the key is currently deleted.</li>
 * </ul>
 *
 * <p>
 * {@code restore} is a general version rollback operation and is not limited
 * to undoing deletions.
 * </p>
 *
 * <h3>Bulk operations</h3>
 * <ul>
 *   <li>Bulk methods apply the same semantic operation as their single-key counterparts.</li>
 *   <li>Bulk operations are executed in the order implied by the input collections.</li>
 *   <li>Returned lists align positionally with the provided inputs.</li>
 * </ul>
 *
 * <h3>Concurrency and transactions</h3>
 * <p>
 * This interface does not prescribe transactional semantics. Implementations may:
 * </p>
 * <ul>
 *   <li>apply operations immediately (e.g. single-user, offline stores), or</li>
 *   <li>defer visibility until an explicit commit phase (e.g. server-side transactional stores).</li>
 * </ul>
 *
 * <p>
 * Callers must not assume atomicity across multiple method calls unless explicitly
 * provided by the implementation.
 * </p>
 *
 * @param <KEY>  logical identifier for stored objects (e.g. path, composite key)
 * @param <I> immutable metadata describing a stored version
 */
public interface Store<KEY, I, O> {

    /**
     * Key/value pair used for bulk creation.
     *
     * @param meta        metadata describing the object to be stored
     * @param inputStream binary content stream; the caller retains ownership and
     *                    is responsible for closing it if required
     */
    public record KV<META>(META meta, InputStream inputStream) {}

    /**
     * Create a new object at the given key.
     *
     * @param key  logical key
     * @param meta metadata describing the object
     * @param in   binary content
     * @return metadata of the created version, or empty if no version was created
     * @throws IOException on I/O failure
     */
    Optional<O> create(KEY key, I meta, InputStream in) throws IOException;

    /**
     * Create a new object with an explicit version identifier.
     *
     * <p>
     * This overload is intended for replication, import, or deterministic
     * replays where the version identifier must be preserved.
     * </p>
     *
     * @param key  logical key
     * @param meta metadata describing the object
     * @param in   binary content
     * @param id   explicit version identifier
     * @return metadata of the created version, or empty if no version was created
     * @throws IOException on I/O failure
     */
    Optional<O> create(KEY key, I meta, InputStream in, String id) throws IOException;

    /**
     * Read the current content associated with the given key.
     *
     * @param key logical key
     * @return content stream if present
     * @throws IOException on I/O failure
     */
    Optional<InputStream> read(KEY key) throws IOException;

    /**
     * Update the content of an existing key.
     *
     * @param key logical key
     * @param is  new content stream
     * @return metadata of the new version, or empty if no update occurred
     * @throws IOException on I/O failure
     */
    Optional<O> update(KEY key, InputStream is) throws IOException;

    /**
     * Soft-delete the object associated with the given key.
     *
     * @param key logical key
     * @return metadata of the delete operation, or empty if no change occurred
     * @throws IOException on I/O failure
     */
    Optional<O> delete(KEY key) throws IOException;

    /**
     * Reverse a prior delete operation.
     *
     * @param key logical key
     * @return metadata of the undelete operation, or empty if not applicable
     * @throws IOException on I/O failure
     */
    Optional<O> undelete(KEY key) throws IOException;

    /**
     * Restore the object at the given key to a prior version.
     *
     * <p>
     * This operation moves the current version (HEAD) to a previous version
     * according to store-specific rules. The key does not need to be deleted
     * for restore to be applicable.
     * </p>
     *
     * @param key logical key
     * @return metadata of the restored version, or empty if no restore occurred
     * @throws IOException on I/O failure
     */
    Optional<O> restore(KEY key) throws IOException;
    
    /**
     * Create multiple objects in a single call.
     *
     * @param kis map of keys to metadata/content pairs
     * @return list of metadata for created versions
     * @throws IOException on I/O failure
     */
    List<O> bulkCreate(Map<KEY, KV<I>> kis) throws IOException;

    /**
     * Create multiple objects with explicit version identifiers.
     *
     * @param kais map of keys to metadata/content pairs
     * @param ids  explicit version identifiers aligned with the input entries
     * @return list of metadata for created versions
     * @throws IOException on I/O failure
     */
    List<O> bulkCreate(Map<KEY, KV<I>> kais, List<String> ids) throws IOException;

    /**
     * Read the current content of multiple keys.
     *
     * @param keys list of logical keys
     * @return list of content streams aligned with the input keys
     * @throws IOException on I/O failure
     */
    List<InputStream> bulkRead(List<KEY> keys) throws IOException;

    /**
     * Update multiple keys with new content.
     *
     * @param mis map of keys to content streams
     * @return list of metadata for updated versions
     * @throws IOException on I/O failure
     */
    List<O> bulkUpdate(Map<KEY, InputStream> mis) throws IOException;

    /**
     * Soft-delete multiple keys.
     *
     * @param keys logical keys
     * @return list of metadata for delete operations
     * @throws IOException on I/O failure
     */
    List<O> bulkDelete(List<KEY> keys) throws IOException;

    /**
     * Reverse deletion for multiple keys.
     *
     * @param keys logical keys
     * @return list of metadata for undelete operations
     * @throws IOException on I/O failure
     */
    List<O> bulkUndelete(List<KEY> keys) throws IOException;

    /**
     * Restore multiple keys to prior versions.
     *
     * <p>
     * Each key is restored independently. Restore is not limited to undoing
     * deletions and may be applied to active keys.
     * </p>
     *
     * @param keys logical keys
     * @return list of metadata for restored versions
     * @throws IOException on I/O failure
     */
    List<O> bulkRestore(List<KEY> keys) throws IOException;

    /**
     * Determine whether the given key has a resolvable latest version.
     *
     * <p>
     * This method returns {@code true} if and only if the store can resolve a
     * current (latest) version for the key (i.e. {@code getLatestVersion(key) != null})
     * and is not deleted {@code versionMeta.operation != "Deleted"} 
     * </p>
     *
     * <p>
     * It does not indicate whether the key existed historically, nor whether
     * older versions may still be present in the store.
     * </p>
     *
     * @param key logical key
     * @return {@code true} if a latest version exists and is not deleted for the key
     * @throws IOException on I/O failure
     */
    boolean exists(KEY key) throws IOException;

}
