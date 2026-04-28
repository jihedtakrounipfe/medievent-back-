package skylinkers.tn.mediconnectbackend.controller.AppointmentController;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import skylinkers.tn.mediconnectbackend.service.AppointmentServices.AnalyticsService;
import skylinkers.tn.mediconnectbackend.service.AppointmentServices.IAppointmentServices;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class DashboardController {

    private final IAppointmentServices appointmentServices;
    private final AnalyticsService analyticsService;

    // ─── Dashboard complet ────────────────────────────────────────────────
    @GetMapping("/{medecin}")
    public ResponseEntity<Map<String, Object>> getDashboard(
            @PathVariable String medecin) {
        return ResponseEntity.ok(appointmentServices.getDashboard(medecin));
    }

    // ─── Stats seules ──────────────────────────────────────────────────────
    @GetMapping("/{medecin}/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @PathVariable String medecin) {
        return ResponseEntity.ok(analyticsService.buildStats(medecin));
    }

    // ─── Tendance 12 mois ─────────────────────────────────────────────────
    @GetMapping("/{medecin}/trend")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyTrend(
            @PathVariable String medecin) {
        return ResponseEntity.ok(analyticsService.buildMonthlyTrend(medecin));
    }

    // ─── Heatmap no-show ──────────────────────────────────────────────────
    @GetMapping("/{medecin}/heatmap")
    public ResponseEntity<List<Map<String, Object>>> getNoShowHeatmap(
            @PathVariable String medecin) {
        return ResponseEntity.ok(analyticsService.buildNoShowByHour(medecin));
    }

    // ─── Spécialités ─────────────────────────────────────────────────────
    @GetMapping("/{medecin}/specialites")
    public ResponseEntity<List<Map<String, Object>>> getSpecialites(
            @PathVariable String medecin) {
        return ResponseEntity.ok(analyticsService.buildSpecialiteBreakdown(medecin));
    }

    // ─── Disponibilités ───────────────────────────────────────────────────
    @GetMapping("/{medecin}/availability")
    public ResponseEntity<List<Map<String, Object>>> getAvailability(
            @PathVariable String medecin) {
        return ResponseEntity.ok(analyticsService.buildWeekAvailability(medecin));
    }
}