package kv.service;


import kv.model.KVEntry;
import kv.model.ReadResponse;
import kv.model.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;

/**
 * HTTP client used by the leader to talk to follower nodes.
 * All calls block so the leader can enforce quorum before responding.
 */
@Service
public class ReplicationClient {

    private static final Logger log = LoggerFactory.getLogger(ReplicationClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;

    public ReplicationClient(WebClient webClient) { this.webClient = webClient; }

    /**
     * Sends a versioned PUT to a follower and blocks until it acknowledges.
     * The follower will sleep 200ms inside its own handler.
     */
    public void replicatePut(String followerUrl, KVEntry entry) {
        WriteResponse req = new WriteResponse(entry.getKey(), entry.getValue(), entry.getVersion());
        try {
            webClient.put()
                    .uri(followerUrl + "/internal/kv")
                    .bodyValue(req)
                    .retrieve()
                    .toBodilessEntity()
                    .block(TIMEOUT);
        } catch (Exception ex) {
            log.error("Replication PUT failed → {}: {}", followerUrl, ex.getMessage());
            throw new RuntimeException("Replication failed for: " + followerUrl, ex);
        }
    }

    /**
     * Forwards a GET to a follower. Used by leader when R > 1.
     * Returns null on error (leader will use its own value as fallback).
     */
    public ReadResponse forwardGet(String followerUrl, String key) {
        try {
            return webClient.get()
                    .uri(followerUrl + "/kv?key=" + key)
                    .retrieve()
                    .bodyToMono(ReadResponse.class)
                    .block(TIMEOUT);
        } catch (Exception ex) {
            log.warn("GET from {} for key '{}' failed: {}", followerUrl, key, ex.getMessage());
            return null;
        }
    }

    /**
     * Reads raw local value from a follower (no delay).
     * Only used by tests via local_read to observe stale data.
     */
    public ReadResponse localRead(String followerUrl, String key) {
        try {
            return webClient.get()
                    .uri(followerUrl + "/internal/kv/local?key=" + key)
                    .retrieve()
                    .bodyToMono(ReadResponse.class)
                    .block(TIMEOUT);
        } catch (Exception ex) {
            log.warn("local_read from {} for key '{}' failed: {}", followerUrl, key, ex.getMessage());
            return null;
        }
    }
}