package skylinkers.tn.mediconnectbackend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import skylinkers.tn.mediconnectbackend.utils.EmailService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DEV-ONLY endpoint — active only when profile = dev or default.
 * Fires ALL email types to a given address so you can validate every template
 * in one HTTP call. Remove or @Profile("prod") before going live.
 *
 * POST /api/dev/email-test?to=takrounipc@gmail.com
 */
@Slf4j
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
@Profile("!prod")
public class DevEmailTestController {

    private final EmailService emailService;

    @PostMapping("/email-test")
    public ResponseEntity<Map<String, String>> fireAllEmails(
            @RequestParam(defaultValue = "takrounipc@gmail.com") String to) {

        log.info("[DEV-TEST] Firing all email types to: {}", to);

        Map<String, String> results = new LinkedHashMap<>();

        // ── 1. Verification code ──────────────────────────────────────────────
        run(results, "1_verification_code", () ->
                emailService.sendVerificationCode(to, "Jihed", "847291"));

        // ── 2. Welcome (after verify) ─────────────────────────────────────────
        run(results, "2_welcome", () ->
                emailService.sendWelcomeEmail(to, "Jihed"));

        // ── 3. Doctor approved ────────────────────────────────────────────────
        run(results, "3_doctor_approved", () ->
                emailService.sendDoctorApprovedEmail(to, "Dr. Jihed Takrouni"));

        // ── 4. Doctor rejected ────────────────────────────────────────────────
        run(results, "4_doctor_rejected", () ->
                emailService.sendDoctorRejectedEmail(to, "Dr. Jihed Takrouni",
                        "Documents de certification incomplets."));

        // ── 5. Participation confirmed ────────────────────────────────────────
        run(results, "5_participation_confirmed", () ->
                emailService.sendParticipationConfirmedEmail(
                        to,
                        "Jihed Takrouni",
                        "Conférence Internationale de Cardiologie 2026",
                        "26/04/2026 à 10:00",
                        "Hôtel Laico — Tunis",
                        "http://localhost:4200/events/1"
                ));

        // ── 6. Participation waitlist ─────────────────────────────────────────
        run(results, "6_participation_waitlist", () ->
                emailService.sendParticipationWaitlistEmail(
                        to,
                        "Jihed Takrouni",
                        "Conférence Internationale de Cardiologie 2026",
                        "26/04/2026 à 10:00",
                        "http://localhost:4200/events/1"
                ));

        // ── 7. Participation promoted from waitlist ────────────────────────────
        run(results, "7_participation_promoted", () ->
                emailService.sendParticipationPromotedEmail(
                        to,
                        "Jihed Takrouni",
                        "Conférence Internationale de Cardiologie 2026",
                        "26/04/2026 à 10:00",
                        "Hôtel Laico — Tunis",
                        "http://localhost:4200/events/1"
                ));

        // ── 8. Guest invitation ───────────────────────────────────────────────
        run(results, "8_guest_invitation", () ->
                emailService.sendGuestInvitationEmail(
                        to,
                        "Jihed Takrouni",
                        "Dr. Ahmed Ben Salah",
                        "Webinaire — Nouvelles approches en Neurologie",
                        "26/04/2026 à 14:00",
                        "http://localhost:4200/events/2/room"
                ));

        // ── 9. Event cancelled ────────────────────────────────────────────────
        run(results, "9_event_cancelled", () ->
                emailService.sendEventCancelledEmail(
                        to,
                        "Jihed Takrouni",
                        "Conférence Internationale de Cardiologie 2026",
                        "26/04/2026 à 10:00"
                ));

        // ── 10. 30-min reminder ───────────────────────────────────────────────
        run(results, "10_event_reminder_30min", () ->
                emailService.sendEventReminderEmail(
                        to,
                        "Jihed Takrouni",
                        "Webinaire — Nouvelles approches en Neurologie",
                        "26/04/2026 à 14:00",
                        "http://localhost:4200/events/2/room"
                ));

        // ── 11. Event has started ─────────────────────────────────────────────
        run(results, "11_event_started", () ->
                emailService.sendEventStartedEmail(
                        to,
                        "Jihed Takrouni",
                        "Webinaire — Nouvelles approches en Neurologie",
                        "http://localhost:4200/events/2/room"
                ));

        // ── 12. Appointment reminder ──────────────────────────────────────────
        run(results, "12_appointment_reminder", () ->
                emailService.sendAppointmentReminderEmail(
                        to,
                        "Jihed Takrouni",
                        "Dr. Ahmed Ben Salah",
                        "26/04/2026 à 09:00"
                ));

        // ── 13. Doctor status change ──────────────────────────────────────────
        run(results, "13_doctor_status_change", () ->
                emailService.sendDoctorStatusChangeEmail(
                        to,
                        "Dr. Jihed Takrouni",
                        "PENDING",
                        "APPROVED",
                        null
                ));

        log.info("[DEV-TEST] All {} email tasks queued (async). Check your inbox: {}", results.size(), to);

        return ResponseEntity.ok(Map.of(
                "destination", to,
                "emailsQueued", String.valueOf(results.size()),
                "status", "All emails dispatched asynchronously — check your inbox in a few seconds.",
                "note", "This endpoint is DEV-only and will not exist in production."
        ));
    }

    /** Wraps each email call so one failure doesn't block the rest. */
    private void run(Map<String, String> results, String name, Runnable task) {
        try {
            task.run();
            results.put(name, "QUEUED");
            log.info("[DEV-TEST]  ✅ {}", name);
        } catch (Exception e) {
            results.put(name, "ERROR: " + e.getMessage());
            log.error("[DEV-TEST]  ❌ {} — {}", name, e.getMessage());
        }
    }
}
