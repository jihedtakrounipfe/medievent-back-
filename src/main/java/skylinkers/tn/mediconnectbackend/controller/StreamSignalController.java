package skylinkers.tn.mediconnectbackend.controller;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * In-memory signaling controller for WebRTC P2P sessions.
 *
 * Architecture (HOST → SPECTATOR push):
 *  1. Spectator registers its PeerJS ID via POST /spectator
 *  2. Host goes live via POST /register, publishing its own PeerJS ID
 *  3. Host polls GET /spectators to discover waiting spectators, then calls them directly
 *  4. On leave: spectator unregisters via DELETE /spectator/{peerId}
 *  5. On session end: host calls DELETE /register to clear everything
 *
 * State is in-memory only – lost on restart (acceptable for live events).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stream")
public class StreamSignalController {

    // eventId → host PeerJS ID (null if not yet live)
    private final ConcurrentHashMap<String, String> hostSession = new ConcurrentHashMap<>();

    // eventId → set of spectator PeerJS IDs waiting to be called
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<String>> spectatorSessions =
            new ConcurrentHashMap<>();

    // ── HOST endpoints ────────────────────────────────────────────────────────

    /**
     * HOST registers its PeerJS ID when going live.
     */
    @PostMapping("/{eventId}/register")
    public ResponseEntity<Object> registerHost(
            @PathVariable String eventId,
            @RequestBody Map<String, String> body) {
        String peerId = body.get("peerId");
        if (peerId == null || peerId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("[SIGNAL] Host {} registered for event {}", peerId, eventId);
        hostSession.put(eventId, peerId);
        return ResponseEntity.ok(Map.of("message", "Host registered"));
    }

    /**
     * HOST retrieves list of spectators currently waiting.
     */
    @GetMapping("/{eventId}/spectators")
    public ResponseEntity<Map<String, Object>> getSpectators(@PathVariable String eventId) {
        Set<String> spectators = spectatorSessions.getOrDefault(eventId, new CopyOnWriteArraySet<>());
        return ResponseEntity.ok(Map.of("spectators", new ArrayList<>(spectators)));
    }

    /**
     * HOST ends the session – clears all state for this event.
     */
    @DeleteMapping("/{eventId}/register")
    public ResponseEntity<Object> clearHost(@PathVariable String eventId) {
        hostSession.remove(eventId);
        spectatorSessions.remove(eventId);
        return ResponseEntity.ok(Map.of("message", "Host session cleared"));
    }

    // ── SPECTATOR endpoints ───────────────────────────────────────────────────

    /**
     * SPECTATOR registers its PeerJS ID so the host can call it.
     */
    @PostMapping("/{eventId}/spectator")
    public ResponseEntity<Map<String, Object>> registerSpectator(
            @PathVariable String eventId,
            @RequestBody Map<String, String> body) {
        String peerId = body.get("peerId");
        if (peerId == null || peerId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        spectatorSessions
                .computeIfAbsent(eventId, k -> new CopyOnWriteArraySet<>())
                .add(peerId);
        log.info("[SIGNAL] Spectator {} registered for event {}", peerId, eventId);

        // Tell specatator if the host is already live
        boolean isLive = hostSession.containsKey(eventId);
        Map<String, Object> response = new HashMap<>();
        response.put("registered", true);
        response.put("hostIsLive", isLive);
        return ResponseEntity.ok(response);
    }

    /**
     * SPECTATOR unregisters when leaving (optional but clean).
     */
    @DeleteMapping("/{eventId}/spectator/{peerId}")
    public ResponseEntity<Object> unregisterSpectator(
            @PathVariable String eventId,
            @PathVariable String peerId) {
        CopyOnWriteArraySet<String> set = spectatorSessions.get(eventId);
        if (set != null) set.remove(peerId);
        return ResponseEntity.ok(Map.of("message", "Spectator unregistered"));
    }

    // ── Legacy signal endpoint (kept for backward compat / diagnostics) ───────

    @GetMapping("/{eventId}/signal")
    public ResponseEntity<Map<String, Object>> getSignal(@PathVariable String eventId) {
        String hostPeerId = hostSession.get(eventId);
        Map<String, Object> response = new HashMap<>();
        response.put("isLive", hostPeerId != null);
        if (hostPeerId != null) response.put("hostPeerId", hostPeerId);
        return ResponseEntity.ok(response);
    }
}
