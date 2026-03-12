package kv.controller;


import kv.model.ReadResponse;
import kv.model.WriteRequest;
import kv.service.KVService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

/**
 * Node-to-node endpoints — restrict these via security groups in AWS,
 * they should never be reachable by end clients.
 *
 *   PUT /internal/kv              — replication: leader → follower
 *   GET /internal/kv/local?key=.. — local_read: no delay, no quorum (testing only)
 */
@RestController
@RequestMapping("/internal/kv")
public class InternalController {

    private final KVService kvService;

    public InternalController(KVService kvService) { this.kvService = kvService; }

    @PutMapping
    public ResponseEntity<Void> replicatePut(@RequestBody WriteRequest request) {
        kvService.applyReplication(request.getKey(), request.getValue(), request.getVersion());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/local")
    public ResponseEntity<?> localRead(@RequestParam String key) {
        Optional<ReadResponse> result = kvService.localRead(key);
        return result.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}