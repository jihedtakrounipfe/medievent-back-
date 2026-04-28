package skylinkers.tn.mediconnectbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import skylinkers.tn.mediconnectbackend.service.security.KeycloakAdminClient;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataMigrationService {

    private final AppUserRepository appUserRepository;
    private final KeycloakAdminClient keycloakAdminClient;
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void runMigrations() {
        migrateNullTwoFactorEnabled();
        migrateAuditLogSchema();
        clearStuckKeycloakRequiredActions();
    }

    private void migrateNullTwoFactorEnabled() {
        try {
            int updated = appUserRepository.fixNullTwoFactorEnabled();
            if (updated > 0) {
                log.info("[Migration] Set two_factor_enabled=false for {} existing user(s) that had NULL.", updated);
            } else {
                log.debug("[Migration] No NULL two_factor_enabled rows found - skipping.");
            }
        } catch (Exception e) {
            log.warn("[Migration] Could not run two_factor_enabled migration: {}", e.getMessage());
        }
    }

    private void migrateAuditLogSchema() {
        ensureAuditLogTableExists();
        ensureAuditLogColumn("action", "VARCHAR(60) NOT NULL");
        ensureAuditLogColumn("details", "VARCHAR(2000) NULL");
        ensureAuditLogColumn("ip_address", "VARCHAR(45) NULL");
        ensureAuditLogColumn("success", "BIT(1) NOT NULL");
        ensureAuditLogColumn("timestamp", "DATETIME(6) NOT NULL");
        ensureAuditLogColumn("user_agent", "TEXT NULL");
        ensureAuditLogColumn("user_id", "BIGINT NULL");
        ensureAuditLogColumn("user_email", "VARCHAR(255) NULL");
        ensureAuditLogColumn("keycloak_id", "VARCHAR(70) NULL");
        ensureAuditLogColumn("category", "VARCHAR(30) NULL");
        ensureAuditLogColumn("risk_score", "INT NULL");
        ensureAuditLogColumn("twofa_decision", "VARCHAR(10) NULL");
        ensureAuditLogColumn("twofa_outcome", "VARCHAR(10) NULL");
        ensureAuditLogColumn("model_version", "VARCHAR(80) NULL");
        ensureAuditLogColumn("feedback_label", "INT NULL");
        ensureIndexIfMissing("idx_audit_user_id", "CREATE INDEX idx_audit_user_id ON mc_audit_log (user_id)");
        ensureIndexIfMissing("idx_audit_timestamp", "CREATE INDEX idx_audit_timestamp ON mc_audit_log (timestamp)");
        ensureIndexIfMissing("idx_audit_action", "CREATE INDEX idx_audit_action ON mc_audit_log (action)");
        ensureAuditLogForeignKey();
        logAuditSchemaSummary();
    }

    private void ensureAuditLogTableExists() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS mc_audit_log (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    action VARCHAR(60) NOT NULL,
                    details VARCHAR(2000) NULL,
                    ip_address VARCHAR(45) NULL,
                    success BIT(1) NOT NULL,
                    timestamp DATETIME(6) NOT NULL,
                    user_agent TEXT NULL,
                    user_id BIGINT NULL,
                    user_email VARCHAR(255) NULL,
                    keycloak_id VARCHAR(70) NULL,
                    category VARCHAR(30) NULL,
                    risk_score INT NULL,
                    twofa_decision VARCHAR(10) NULL,
                    twofa_outcome VARCHAR(10) NULL,
                    model_version VARCHAR(80) NULL,
                    feedback_label INT NULL,
                    PRIMARY KEY (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
                """);
    }

    private void ensureAuditLogColumn(String columnName, String definition) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE()
                      AND table_name = 'mc_audit_log'
                      AND column_name = ?
                    """,
                    Integer.class,
                    columnName
            );

            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE mc_audit_log ADD COLUMN " + columnName + " " + definition);
                log.info("[Migration] Added mc_audit_log.{}.", columnName);
            } else {
                jdbcTemplate.execute("ALTER TABLE mc_audit_log MODIFY COLUMN " + columnName + " " + definition);
            }
        } catch (Exception e) {
            log.warn("[Migration] Column migration failed for mc_audit_log.{}: {}", columnName, e.getMessage(), e);
        }
    }

    private void ensureIndexIfMissing(String indexName, String createSql) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                      AND table_name = 'mc_audit_log'
                      AND index_name = ?
                    """,
                    Integer.class,
                    indexName
            );

            if (count != null && count == 0) {
                jdbcTemplate.execute(createSql);
                log.info("[Migration] Added index {} on mc_audit_log.", indexName);
            }
        } catch (Exception e) {
            log.warn("[Migration] Index migration failed for {}: {}", indexName, e.getMessage(), e);
        }
    }

    private void ensureAuditLogForeignKey() {
        try {
            dropForeignKeyIfExists("fk_mc_audit_log_user");
            dropForeignKeyIfExists("FK9dyraoq5bpx2v2pjl6797umgg");
            jdbcTemplate.execute("""
                    ALTER TABLE mc_audit_log
                    ADD CONSTRAINT fk_mc_audit_log_user
                    FOREIGN KEY (user_id) REFERENCES users(id)
                    ON DELETE SET NULL
                    """);
            log.info("[Migration] Ensured mc_audit_log foreign key with ON DELETE SET NULL.");
        } catch (Exception e) {
            log.warn("[Migration] Foreign key refresh failed for mc_audit_log: {}", e.getMessage(), e);
            rebuildAuditLogTable();
        }
    }

    private void dropForeignKeyIfExists(String constraintName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM information_schema.table_constraints
                    WHERE constraint_schema = DATABASE()
                      AND table_name = 'mc_audit_log'
                      AND constraint_name = ?
                      AND constraint_type = 'FOREIGN KEY'
                    """,
                    Integer.class,
                    constraintName
            );

            if (count != null && count > 0) {
                jdbcTemplate.execute("ALTER TABLE mc_audit_log DROP FOREIGN KEY " + constraintName);
            }
        } catch (Exception e) {
            log.warn("[Migration] Could not drop mc_audit_log FK {}: {}", constraintName, e.getMessage(), e);
        }
    }

    private void rebuildAuditLogTable() {
        String backupTable = "mc_audit_log_backup_" + LocalDateTime.now()
                .withNano(0)
                .toString()
                .replace(":", "")
                .replace("-", "")
                .replace("T", "_");

        try {
            jdbcTemplate.execute("RENAME TABLE mc_audit_log TO " + backupTable);
            ensureAuditLogTableExists();
            ensureAuditLogColumn("action", "VARCHAR(60) NOT NULL");
            ensureAuditLogColumn("details", "VARCHAR(2000) NULL");
            ensureAuditLogColumn("ip_address", "VARCHAR(45) NULL");
            ensureAuditLogColumn("success", "BIT(1) NOT NULL");
            ensureAuditLogColumn("timestamp", "DATETIME(6) NOT NULL");
            ensureAuditLogColumn("user_agent", "TEXT NULL");
            ensureAuditLogColumn("user_id", "BIGINT NULL");
            ensureAuditLogColumn("user_email", "VARCHAR(255) NULL");
            ensureAuditLogColumn("keycloak_id", "VARCHAR(70) NULL");
            ensureAuditLogColumn("category", "VARCHAR(30) NULL");
            ensureAuditLogColumn("risk_score", "INT NULL");
            ensureAuditLogColumn("twofa_decision", "VARCHAR(10) NULL");
            ensureAuditLogColumn("twofa_outcome", "VARCHAR(10) NULL");
            ensureAuditLogColumn("model_version", "VARCHAR(80) NULL");
            ensureAuditLogColumn("feedback_label", "INT NULL");
            ensureIndexIfMissing("idx_audit_user_id", "CREATE INDEX idx_audit_user_id ON mc_audit_log (user_id)");
            ensureIndexIfMissing("idx_audit_timestamp", "CREATE INDEX idx_audit_timestamp ON mc_audit_log (timestamp)");
            ensureIndexIfMissing("idx_audit_action", "CREATE INDEX idx_audit_action ON mc_audit_log (action)");

            jdbcTemplate.execute("""
                    INSERT INTO mc_audit_log (
                        id, action, details, ip_address, success, timestamp, user_agent, user_id,
                        user_email, keycloak_id, category, risk_score, twofa_decision,
                        twofa_outcome, model_version, feedback_label
                    )
                    SELECT
                        id,
                        action,
                        LEFT(details, 2000),
                        ip_address,
                        success,
                        timestamp,
                        user_agent,
                        user_id,
                        NULL,
                        NULL,
                        NULL,
                        NULL,
                        NULL,
                        NULL,
                        NULL,
                        NULL
                    FROM """
                    + backupTable);

            Long nextId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM mc_audit_log", Long.class);
            if (nextId != null && nextId > 1) {
                jdbcTemplate.execute("ALTER TABLE mc_audit_log AUTO_INCREMENT = " + nextId);
            }

            dropForeignKeyIfExists("fk_mc_audit_log_user");
            jdbcTemplate.execute("""
                    ALTER TABLE mc_audit_log
                    ADD CONSTRAINT fk_mc_audit_log_user
                    FOREIGN KEY (user_id) REFERENCES users(id)
                    ON DELETE SET NULL
                    """);

            log.warn("[Migration] Rebuilt mc_audit_log successfully. Legacy copy kept in {}.", backupTable);
        } catch (Exception rebuildError) {
            log.error("[Migration] Rebuild of mc_audit_log failed: {}", rebuildError.getMessage(), rebuildError);
        }
    }

    private void logAuditSchemaSummary() {
        try {
            String nullable = jdbcTemplate.queryForObject(
                    """
                    SELECT is_nullable
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE()
                      AND table_name = 'mc_audit_log'
                      AND column_name = 'user_id'
                    """,
                    String.class
            );
            log.info("[Migration] mc_audit_log ready. user_id nullable={}", "YES".equalsIgnoreCase(nullable));
        } catch (Exception e) {
            log.warn("[Migration] Could not summarize mc_audit_log schema: {}", e.getMessage(), e);
        }
    }

    private void clearStuckKeycloakRequiredActions() {
        try {
            List<AppUser> users = appUserRepository.findAll();
            int cleaned = 0;
            for (AppUser user : users) {
                if (user.getKeycloakId() == null || "TEMP".equals(user.getKeycloakId())) {
                    continue;
                }
                try {
                    keycloakAdminClient.clearRequiredActions(user.getKeycloakId());
                    cleaned++;
                } catch (Exception e) {
                    log.debug("[Migration] clearRequiredActions skipped for keycloakId={}: {}",
                            user.getKeycloakId(), e.getMessage());
                }
            }
            if (cleaned > 0) {
                log.info("[Migration] Cleared required actions for {} Keycloak user(s).", cleaned);
            }
        } catch (Exception e) {
            log.warn("[Migration] clearStuckKeycloakRequiredActions failed (Keycloak may be unavailable): {}",
                    e.getMessage());
        }
    }
}
