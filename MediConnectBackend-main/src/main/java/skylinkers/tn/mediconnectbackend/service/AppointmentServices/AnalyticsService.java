package skylinkers.tn.mediconnectbackend.service.AppointmentServices;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import skylinkers.tn.mediconnectbackend.entities.enums.AppointmentStatus;
import skylinkers.tn.mediconnectbackend.entities.Appointment;
import skylinkers.tn.mediconnectbackend.repository.AppointmentRepository.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AppointmentRepository appointmentRepository;

    private static final int TOTAL_SLOTS = 12;
    private static final String[] MONTH_LABELS = {
            "Jan","Fév","Mar","Avr","Mai","Jun",
            "Jul","Aoû","Sep","Oct","Nov","Déc"
    };
    private static final String[] DAY_LABELS = {
            "Lun","Mar","Mer","Jeu","Ven","Sam","Dim"
    };

    public Map<String, Object> getFullDashboard(String medecin) {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("stats",                buildStats(medecin));
        dashboard.put("weeklyData",           buildWeeklyData(medecin));
        dashboard.put("monthlyTrend",         buildMonthlyTrend(medecin));
        dashboard.put("noShowByHour",         buildNoShowByHour(medecin));
        dashboard.put("specialiteBreakdown",  buildSpecialiteBreakdown(medecin));
        dashboard.put("weekAvailability",     buildWeekAvailability(medecin));
        dashboard.put("upcomingAppointments", buildUpcoming(medecin));
        return dashboard;
    }

    public Map<String, Object> buildStats(String medecin) {
        long pending    = appointmentRepository.countByMedecinAndStatus(medecin, AppointmentStatus.PENDING);
        long confirmed  = appointmentRepository.countByMedecinAndStatus(medecin, AppointmentStatus.CONFIRMED);
        long done       = appointmentRepository.countByMedecinAndStatus(medecin, AppointmentStatus.DONE);
        long cancelled  = appointmentRepository.countByMedecinAndStatus(medecin, AppointmentStatus.CANCELLED);
        long noShowHigh = appointmentRepository.countByMedecinAndStatus(medecin, AppointmentStatus.NO_SHOWHIGH);
        long noShowMed  = appointmentRepository.countByMedecinAndStatus(medecin, AppointmentStatus.NO_SHOWMedium);
        long total      = appointmentRepository.countByMedecin(medecin);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("pending",      pending);
        stats.put("confirmed",    confirmed);
        stats.put("done",         done);
        stats.put("cancelled",    cancelled);
        stats.put("noShowHigh",   noShowHigh);
        stats.put("noShowMedium", noShowMed);
        stats.put("total",        total);
        stats.put("noShowRate",   total > 0
                ? Math.round((double)(noShowHigh + noShowMed + cancelled) / total * 1000.0) / 10.0
                : 0.0);
        return stats;
    }

    public Map<String, Object> buildWeeklyData(String medecin) {
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);

        List<Object[]> raw = appointmentRepository.countConfirmedByDay(medecin, monday, sunday);

        Map<LocalDate, Integer> byDate = raw.stream()
                .collect(Collectors.toMap(
                        r -> (LocalDate) r[0],
                        r -> ((Number) r[1]).intValue()
                ));

        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            data.add(byDate.getOrDefault(monday.plusDays(i), 0));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", Arrays.asList(DAY_LABELS));
        result.put("data",   data);
        return result;
    }

    public List<Map<String, Object>> buildMonthlyTrend(String medecin) {
        return appointmentRepository.getMonthlyTrend(medecin)
                .stream()
                .map(r -> {
                    int month  = ((Number) r[0]).intValue();
                    int year   = ((Number) r[1]).intValue();
                    int total  = ((Number) r[2]).intValue();
                    int noshow = r[3] != null ? ((Number) r[3]).intValue() : 0;

                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("month",      month);
                    point.put("year",       year);
                    point.put("label",      MONTH_LABELS[month - 1] + " " + year);
                    point.put("total",      total);
                    point.put("noshow",     noshow);
                    point.put("noShowRate", total > 0
                            ? Math.round((double) noshow / total * 1000.0) / 10.0
                            : 0.0);
                    return point;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> buildNoShowByHour(String medecin) {
        return appointmentRepository.getNoShowByHour(medecin)
                .stream()
                .map(r -> {
                    int hour   = ((Number) r[0]).intValue();
                    int total  = ((Number) r[1]).intValue();
                    int noshow = r[2] != null ? ((Number) r[2]).intValue() : 0;

                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("hour",       hour);
                    point.put("label",      hour + "h");
                    point.put("total",      total);
                    point.put("noshow",     noshow);
                    point.put("noShowRate", total > 0
                            ? Math.round((double) noshow / total * 1000.0) / 10.0
                            : 0.0);
                    return point;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> buildSpecialiteBreakdown(String medecin) {
        return appointmentRepository.countBySpecialite(medecin)
                .stream()
                .map(r -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("specialite", r[0]);
                    item.put("count",      ((Number) r[1]).intValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> buildWeekAvailability(String medecin) {
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);

        List<Object[]> raw = appointmentRepository.getTakenSlotsByDay(medecin, monday, sunday);

        Map<LocalDate, Integer> takenMap = raw.stream()
                .collect(Collectors.toMap(
                        r -> (LocalDate) r[0],
                        r -> ((Number) r[1]).intValue()
                ));

        return IntStream.range(0, 7).mapToObj(i -> {
            LocalDate day = monday.plusDays(i);
            int taken     = takenMap.getOrDefault(day, 0);
            int free      = Math.max(0, TOTAL_SLOTS - taken);

            Map<String, Object> dayMap = new LinkedHashMap<>();
            dayMap.put("date",       day.toString());
            dayMap.put("dayLabel",   DAY_LABELS[i]);
            dayMap.put("takenSlots", taken);
            dayMap.put("freeSlots",  free);
            dayMap.put("totalSlots", TOTAL_SLOTS);
            return dayMap;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> buildUpcoming(String medecin) {
        return appointmentRepository
                .findByMedecinAndStatusAndDateGreaterThanEqualOrderByDateAscHeureAsc(
                        medecin, AppointmentStatus.CONFIRMED, LocalDate.now()
                )
                .stream()
                .limit(5)
                .map(a -> {
                    Map<String, Object> dto = new LinkedHashMap<>();
                    dto.put("idAppointment", a.getIdAppointment());
                    dto.put("date",          a.getDate());
                    dto.put("heure",         a.getHeure());
                    dto.put("specialite",    a.getSpecialite());
                    dto.put("motif",         a.getMotif());
                    dto.put("status",        a.getStatus());
                    dto.put("medecin",       a.getMedecin());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}