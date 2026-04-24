package skylinkers.tn.mediconnectbackend.controller;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory signaling controller for WebRTC P2P sessions with Heartbeat and Hand Raise.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stream")
public class StreamSignalController {

    private static final long STALE_TIMEOUT_MS = 15000; // 15 seconds

    // eventId -> Map<peerId, lastSeenTimestamp>
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> hostSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> spectatorSessions = new ConcurrentHashMap<>();
    
    // eventId -> List of HandRaise (PeerID + Name)
    private final ConcurrentHashMap<String, List<Map<String, String>>> pendingHandRaises = new ConcurrentHashMap<>();

    private void cleanupStale(ConcurrentHashMap<String, Long> sessions, String eventId, boolean isHost) {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> {
            boolean isStale = (now - entry.getValue()) > STALE_TIMEOUT_MS;
            if (isStale && !isHost) {
                // If spectator is stale, also remove their hand raise
                removeHandRaise(eventId, entry.getKey());
            }
            return isStale;
        });
    }

    private void removeHandRaise(String eventId, String peerId) {
        List<Map<String, String>> list = pendingHandRaises.get(eventId);
        if (list != null) {
            list.removeIf(hr -> peerId.equals(hr.get("peerId")));
        }
    }

    // ── HOST endpoints ────────────────────────────────────────────────────────

    @PostMapping("/{eventId}/register")
    public ResponseEntity<Object> registerHost(@PathVariable String eventId, @RequestBody Map<String, String> body) {
        String peerId = body.get("peerId");
        if (peerId == null || peerId.isBlank()) return ResponseEntity.badRequest().build();
        hostSessions.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>()).put(peerId, System.currentTimeMillis());
        return ResponseEntity.ok(Map.of("message", "Host heartbeat"));
    }

    @GetMapping("/{eventId}/spectators")
    public ResponseEntity<Map<String, Object>> getSpectators(@PathVariable String eventId) {
        ConcurrentHashMap<String, Long> spectators = spectatorSessions.getOrDefault(eventId, new ConcurrentHashMap<>());
        cleanupStale(spectators, eventId, false);
        return ResponseEntity.ok(Map.of("spectators", new ArrayList<>(spectators.keySet())));
    }

    @DeleteMapping("/{eventId}/register/{peerId}")
    public ResponseEntity<Object> unregisterHost(@PathVariable String eventId, @PathVariable String peerId) {
        ConcurrentHashMap<String, Long> set = hostSessions.get(eventId);
        if (set != null) set.remove(peerId);
        return ResponseEntity.ok(Map.of("message", "Host removed"));
    }

    // ── SPECTATOR endpoints ───────────────────────────────────────────────────

    @PostMapping("/{eventId}/spectator")
    public ResponseEntity<Map<String, Object>> registerSpectator(@PathVariable String eventId, @RequestBody Map<String, String> body) {
        String peerId = body.get("peerId");
        if (peerId == null || peerId.isBlank()) return ResponseEntity.badRequest().build();
        spectatorSessions.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>()).put(peerId, System.currentTimeMillis());
        
        ConcurrentHashMap<String, Long> hosts = hostSessions.getOrDefault(eventId, new ConcurrentHashMap<>());
        cleanupStale(hosts, eventId, true);
        
        return ResponseEntity.ok(Map.of("registered", true, "hostIsLive", !hosts.isEmpty()));
    }

    @DeleteMapping("/{eventId}/spectator/{peerId}")
    public ResponseEntity<Object> unregisterSpectator(@PathVariable String eventId, @PathVariable String peerId) {
        ConcurrentHashMap<String, Long> set = spectatorSessions.get(eventId);
        if (set != null) set.remove(peerId);
        removeHandRaise(eventId, peerId);
        return ResponseEntity.ok(Map.of("message", "Spectator removed"));
    }

    // ── HAND RAISE endpoints ──────────────────────────────────────────────────

    @PostMapping("/{eventId}/hand-raise")
    public ResponseEntity<Object> requestToSpeak(@PathVariable String eventId, @RequestBody Map<String, String> body) {
        String peerId = body.get("peerId");
        String name = body.get("name");
        if (peerId == null || name == null) return ResponseEntity.badRequest().build();
        
        List<Map<String, String>> list = pendingHandRaises.computeIfAbsent(eventId, k -> Collections.synchronizedList(new ArrayList<>()));
        // Avoid duplicates
        list.removeIf(hr -> peerId.equals(hr.get("peerId")));
        list.add(Map.of("peerId", peerId, "name", name));
        
        return ResponseEntity.ok(Map.of("status", "requested"));
    }

    @GetMapping("/{eventId}/hand-raises")
    public ResponseEntity<List<Map<String, String>>> getHandRaises(@PathVariable String eventId) {
        return ResponseEntity.ok(pendingHandRaises.getOrDefault(eventId, new ArrayList<>()));
    }

    @PostMapping("/{eventId}/hand-raise/action")
    public ResponseEntity<Object> handleHandRaiseAction(@PathVariable String eventId, @RequestBody Map<String, String> body) {
        String peerId = body.get("peerId");
        String action = body.get("action"); // "ACCEPT" or "REJECT"
        if (peerId == null || action == null) return ResponseEntity.badRequest().build();
        
        removeHandRaise(eventId, peerId);
        
        if ("ACCEPT".equalsIgnoreCase(action)) {
            // Promote to host session
            hostSessions.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>()).put(peerId, System.currentTimeMillis());
            return ResponseEntity.ok(Map.of("status", "accepted"));
        }
        return ResponseEntity.ok(Map.of("status", "rejected"));
    }

    @GetMapping("/{eventId}/signal")
    public ResponseEntity<Map<String, Object>> getSignal(@PathVariable String eventId) {
        ConcurrentHashMap<String, Long> hosts = hostSessions.getOrDefault(eventId, new ConcurrentHashMap<>());
        cleanupStale(hosts, eventId, true);
        return ResponseEntity.ok(Map.of("isLive", !hosts.isEmpty(), "hosts", new ArrayList<>(hosts.keySet())));
    }
}
