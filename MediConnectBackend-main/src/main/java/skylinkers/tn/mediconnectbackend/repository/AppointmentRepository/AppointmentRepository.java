package skylinkers.tn.mediconnectbackend.repository.AppointmentRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import skylinkers.tn.mediconnectbackend.entities.enums.*;
import skylinkers.tn.mediconnectbackend.entities.enums.AppointmentStatus;
import skylinkers.tn.mediconnectbackend.entities.Appointment;
import skylinkers.tn.mediconnectbackend.entities.Patient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatient(Patient patient);



    // ─── Existants ────────────────────────────────────────────────────────
    List<Appointment> findByMedecin(String medecin);
    List<Appointment> findByStatusNot(AppointmentStatus status);

    // ─── Stats globales ───────────────────────────────────────────────────
    long countByMedecinAndStatus(String medecin, AppointmentStatus status);
    long countByMedecin(String medecin);


    // ─── Prochains RDV confirmés ──────────────────────────────────────────
    List<Appointment> findByMedecinAndStatusAndDateGreaterThanEqualOrderByDateAscHeureAsc(
            String medecin, AppointmentStatus status, LocalDate date
    );

    // ─── Consultations confirmées par jour ────────────────────────────────
    @Query("""
        SELECT a.date, COUNT(a)
        FROM Appointment a
        WHERE a.medecin = :medecin
          AND a.status = 'CONFIRMED'
          AND a.date BETWEEN :from AND :to
        GROUP BY a.date
        ORDER BY a.date
    """)
    List<Object[]> countConfirmedByDay(
            @Param("medecin") String medecin,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    // ─── Tendance mensuelle 12 mois ───────────────────────────────────────
    @Query(value = "SELECT MONTH(date) AS month, YEAR(date) AS year, COUNT(*) AS total, SUM(CASE WHEN status IN ('CANCELLED','NO_SHOWHIGH','NO_SHOWMedium') THEN 1 ELSE 0 END) AS noshow FROM appointment WHERE medecin = :medecin AND date >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) GROUP BY YEAR(date), MONTH(date) ORDER BY YEAR(date), MONTH(date)", nativeQuery = true)
    List<Object[]> getMonthlyTrend(@Param("medecin") String medecin);

    // ─── No-show par heure ────────────────────────────────────────────────
    @Query(value = "SELECT HOUR(heure) AS hour, COUNT(*) AS total, SUM(CASE WHEN status IN ('CANCELLED','NO_SHOWHIGH','NO_SHOWMedium') THEN 1 ELSE 0 END) AS noshow FROM appointment WHERE medecin = :medecin AND date >= DATE_SUB(CURDATE(), INTERVAL 3 MONTH) GROUP BY HOUR(heure) ORDER BY HOUR(heure)", nativeQuery = true)
    List<Object[]> getNoShowByHour(@Param("medecin") String medecin);

    // ─── Répartition par spécialité ───────────────────────────────────────
    @Query("""
        SELECT a.specialite, COUNT(a)
        FROM Appointment a
        WHERE a.medecin = :medecin
          AND a.specialite IS NOT NULL
        GROUP BY a.specialite
        ORDER BY COUNT(a) DESC
    """)
    List<Object[]> countBySpecialite(@Param("medecin") String medecin);

    // ─── Créneaux pris par jour ───────────────────────────────────────────
    @Query("""
        SELECT a.date, COUNT(a)
        FROM Appointment a
        WHERE a.medecin = :medecin
          AND a.date BETWEEN :from AND :to
          AND a.status IN ('CONFIRMED', 'PENDING')
        GROUP BY a.date
    """)
    List<Object[]> getTakenSlotsByDay(
            @Param("medecin") String medecin,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );




    // Trouver les RDV à décaler (CONFIRMED, même jour, >= slotLibre)
    @Query("SELECT a FROM Appointment a WHERE a.medecin = :medecin AND a.date = :date " +
            "AND a.heure >= :depuis AND a.status = 'CONFIRMED' AND a.urgencyLevel = 'NORMAL' " +
            "ORDER BY a.heure ASC")
    List<Appointment> findAppointmentsToReschedule(
            @Param("medecin") String medecin,
            @Param("date") LocalDate date,
            @Param("depuis") LocalTime depuis
    );

    // Trouver tous les RDV du jour triés par heure
    List<Appointment> findByMedecinAndDateOrderByHeureAsc(String medecin, LocalDate date);

    List<Appointment> findByDateAndStatus(LocalDate date, AppointmentStatus status);


    // Dans AppointmentRepository

    int countByMedecinAndDateAndStatus(
            String medecin, LocalDate date, AppointmentStatus status);

    List<Appointment> findByMedecinAndDateAndStatus(
            String medecin, LocalDate date, AppointmentStatus status);

    @Query("""
    SELECT a FROM Appointment a
    WHERE a.medecin = :medecin
      AND a.date = :date
      AND a.status IN :statuses
    ORDER BY
      CASE a.urgencyLevel WHEN 'URGENT' THEN 0 ELSE 1 END ASC,
      a.heure ASC
""")
    List<Appointment> findQueueByMedecinAndDate(
            @Param("medecin") String medecin,
            @Param("date") LocalDate date,
            @Param("statuses") List<AppointmentStatus> statuses
    );

    List<Appointment> findByPatientAndDateAndStatus(Patient patient, LocalDate date, AppointmentStatus status);


}