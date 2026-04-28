package skylinkers.tn.mediconnectbackend.seeder.SubscriptionSeeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StudentVerificationSchemaSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        makeStudentIdNumberNullable();
        ensureExpiresAtColumnExists();
    }

    private void makeStudentIdNumberNullable() {
        try {
            String isNullable = jdbcTemplate.queryForObject(
                    "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() " +
                            "AND TABLE_NAME = 'student_verifications' " +
                            "AND COLUMN_NAME = 'student_id_number'",
                    String.class
            );

            if (isNullable == null) {
                log.warn("Column student_verifications.student_id_number was not found; skipping schema update");
                return;
            }

            if ("YES".equalsIgnoreCase(isNullable)) {
                return;
            }

            jdbcTemplate.execute(
                    "ALTER TABLE student_verifications MODIFY COLUMN student_id_number VARCHAR(255) NULL"
            );
            log.info("Updated student_verifications.student_id_number to allow NULL values");
        } catch (Exception e) {
            log.warn("Could not update student_verifications.student_id_number nullability automatically: {}", e.getMessage());
        }
    }

    private void ensureExpiresAtColumnExists() {
        try {
            Integer columnExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() " +
                            "AND TABLE_NAME = 'student_verifications' " +
                            "AND COLUMN_NAME = 'expires_at'",
                    Integer.class
            );

            if (columnExists != null && columnExists > 0) {
                return;
            }

            jdbcTemplate.execute(
                    "ALTER TABLE student_verifications ADD COLUMN expires_at DATETIME NULL"
            );
            log.info("Added student_verifications.expires_at column");
        } catch (Exception e) {
            log.warn("Could not add student_verifications.expires_at column automatically: {}", e.getMessage());
        }
    }
}