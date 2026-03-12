package kv.service;


import kv.config.NodeConfig;
import kv.model.KVEntry;
import kv.model.ReadResponse;
import kv.model.WriteRequest;
import kv.model.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

/**
 * Core logic for all three W/R configurations:
 *
 *   W=5, R=1 — replicate to all 4 followers (sequential, each sleeps 200ms)
 *              then leader sleeps 200ms → ~1s total. Read only hits leader.
 *
 *   W=1, R=1 — leader writes itself (200ms) and returns immediately.
 *              Followers have NOT been updated yet → inconsistency window.
 *
 *   W=3, R=3 — replicate to 2 followers + self for writes (≥600ms).
 *              Read queries 2 followers + self, returns highest version.
 *
 * Write replication is always SEQUENTIAL so the inconsistency window
 * is large enough to observe in load tests.
 */
@Service
public class KVService {

    private static final Logger log = LoggerFactory.getLogger(KVService.class);

    private final NodeConfig config;
    private final LocalStoreService store;
    private final ReplicationClient replicationClient;

    public KVService(NodeConfig config, LocalStoreService store, ReplicationClient replicationClient) {
        this.config = config;
        this.store = store;
        this.replicationClient = replicationClient;
    }

    // ------------------------------------------------------------------
    // Client write — must only be called on the leader
    // ------------------------------------------------------------------

    public WriteResponse write(String key, String value) {
        if (!config.isLeader()) {
            throw new IllegalStateException("Writes must go to the leader node");
        }

        int version = store.nextVersion(key);
        KVEntry entry = new KVEntry(key, value, version);

        // Sequentially replicate to (W-1) followers
        List<String> followers = config.getFollowerUrls();
        int followersToUpdate = Math.min(config.getWriteQuorum() - 1, followers.size());
        for (int i = 0; i < followersToUpdate; i++) {
            log.debug("Replicating key='{}' v={} → {}", key, version, followers.get(i));
            replicationClient.replicatePut(followers.get(i), entry);
            // Each follower already slept 200ms inside its own handler
        }

        // Leader simulates its own durable write
        sleep(config.getWriteDelayMs());
        store.put(entry);

        log.info("Write done: key='{}' v={} W={}", key, version, config.getWriteQuorum());
        return new WriteResponse(key, version);
    }

    // ------------------------------------------------------------------
    // Client read — works on any node; quorum logic runs on leader only
    // ------------------------------------------------------------------

    public Optional<ReadResponse> read(String key) {
        sleep(config.getReadDelayMs());

        // Follower or R=1: just return local value
        if (!config.isLeader() || config.getReadQuorum() == 1) {
            return store.get(key).map(e -> new ReadResponse(e.getKey(), e.getValue(), e.getVersion()));
        }

        // R > 1: collect from (R-1) followers, return highest version
        ReadResponse best = store.get(key)
                .map(e -> new ReadResponse(e.getKey(), e.getValue(), e.getVersion()))
                .orElse(null);

        List<String> followers = config.getFollowerUrls();
        int followersToQuery = Math.min(config.getReadQuorum() - 1, followers.size());
        for (int i = 0; i < followersToQuery; i++) {
            ReadResponse resp = replicationClient.forwardGet(followers.get(i), key);
            if (resp != null && (best == null || resp.getVersion() > best.getVersion())) {
                best = resp;
            }
        }

        return Optional.ofNullable(best);
    }

    // ------------------------------------------------------------------
    // Internal replication — called on followers via PUT /internal/kv
    // ------------------------------------------------------------------

    public void applyReplication(String key, String value, int version) {
        sleep(config.getWriteDelayMs()); // simulate durable write on this node
        store.put(new KVEntry(key, value, version));
        log.debug("Replication applied: key='{}' v={}", key, version);
    }

    // ------------------------------------------------------------------
    // Testing helper: local_read (no delay, no quorum)
    // ------------------------------------------------------------------

    /**
     * Returns whatever this node currently holds — no artificial delay.
     * Used by unit tests to observe stale data during the inconsistency window.
     */
    public Optional<ReadResponse> localRead(String key) {
        return store.get(key).map(e -> new ReadResponse(e.getKey(), e.getValue(), e.getVersion()));
    }

    // ------------------------------------------------------------------

    private void sleep(long millis) {
        try { Thread.sleep(millis); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}