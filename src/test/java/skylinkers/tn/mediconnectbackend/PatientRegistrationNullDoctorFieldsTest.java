package skylinkers.tn.mediconnectbackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import skylinkers.tn.mediconnectbackend.dto.request.CreatePatientRequest;
import skylinkers.tn.mediconnectbackend.entities.enums.Gender;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.PatientService;

import java.sql.ResultSet;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@ActiveProfiles("dev")
class PatientRegistrationNullDoctorFieldsTest {

    @Autowired
    private PatientService patientService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void registeringPatientLeavesDoctorColumnsNull() {
        CreatePatientRequest req = new CreatePatientRequest();
        req.setEmail("patient.null.doctor.fields+" + System.currentTimeMillis() + "@example.com");
        req.setPassword("DummyPassword1!");
        req.setKeycloakId("TEST-" + System.currentTimeMillis());
        req.setFirstName("Test");
        req.setLastName("Patient");
        req.setPhone("+21600000000");
        req.setDateOfBirth(LocalDate.of(1999, 1, 1));
        req.setGender(Gender.MALE);

        var saved = patientService.createPatient(req);

        jdbc.query("SELECT specialization, consultation_duration, rpps_number, office_address, verification_status, license_number, google_calendar_linked, clinic_id FROM users WHERE id = ?",
                ps -> ps.setLong(1, saved.getId()),
                (ResultSet rs) -> {
                    if (rs.next()) {
                        assertNull(rs.getObject("specialization"));
                        assertNull(rs.getObject("consultation_duration"));
                        assertNull(rs.getObject("rpps_number"));
                        assertNull(rs.getObject("office_address"));
                        assertNull(rs.getObject("verification_status"));
                        assertNull(rs.getObject("license_number"));
                        assertNull(rs.getObject("google_calendar_linked"));
                        assertNull(rs.getObject("clinic_id"));
                    }
                });
    }
}

