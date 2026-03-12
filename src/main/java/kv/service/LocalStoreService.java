package kv.service;

import kv.model.KVEntry;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory store. Every node has one of these.
 * All data is intentionally lost on restart (spec requirement).
 */
@Service
public class LocalStoreService {

    private final ConcurrentHashMap<String, KVEntry> store = new ConcurrentHashMap<>();

    /** Stores the entry unconditionally (called by leader and followers). */
    public void put(KVEntry entry) {
        store.put(entry.getKey(), entry);
    }

    /** Returns the locally stored entry, if present. */
    public Optional<KVEntry> get(String key) { //may get nothing , so use this option
        return Optional.ofNullable(store.get(key));
    }

    /**
     * Computes the next version number for a key.
     * Called by the leader before it propagates the write.
     */
    public int nextVersion(String key) {
        KVEntry existing = store.get(key);
        return (existing == null) ? 1 : existing.getVersion() + 1; // updating the version, if it is the first, it is 1.
    }
}