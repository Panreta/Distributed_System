package kv.controller;


import kv.model.ReadResponse;
import kv.model.WriteResponse;
import kv.model.WriteRequest;
import kv.service.KVService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

/**
 * Public endpoints:
 *   PUT /kv   { "key": "...", "value": "..." }  →  201 { "key", "version" }
 *   GET /kv?key=...                             →  200 { "key", "value", "version" }  |  404
 *
 * All writes must be directed to the leader's IP.
 * Reads can go to any node (or through the ALB).
 */
@RestController
@RequestMapping("/kv")
public class KVController {

    private final KVService kvService;

    public KVController(KVService kvService) { this.kvService = kvService; }

    @PutMapping
    public ResponseEntity<?> put(@RequestBody WriteResponse request) {
        if (request.getKey() == null || request.getKey().isEmpty()) {
            return ResponseEntity.badRequest().body("key must not be empty");
        }
        WriteRequest response = kvService.write(request.getKey(), request.getValue());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<?> get(@RequestParam String key) {
        Optional<ReadResponse> result = kvService.read(key);
        return result.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}