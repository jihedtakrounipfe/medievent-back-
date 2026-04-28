package skylinkers.tn.mediconnectbackend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import skylinkers.tn.mediconnectbackend.entities.AuditLog;
import skylinkers.tn.mediconnectbackend.entities.Patient;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AuditLogRepository;
import skylinkers.tn.mediconnectbackend.service.UserServices.UserImpl.AuditLogServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    private AuditLogServiceImpl auditLogService;

    @BeforeEach
    void setUp() {
        TransactionStatus transactionStatus = new SimpleTransactionStatus();
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        auditLogService = new AuditLogServiceImpl(auditLogRepository, appUserRepository, transactionManager);
    }

    @Test
    void storesDenormalizedDataWhenUserIsNotVisibleInAuditTransaction() {
        Patient requestUser = new Patient();
        requestUser.setId(42L);
        requestUser.setEmail("patient@example.com");
        requestUser.setKeycloakId("kc-42");

        when(appUserRepository.findById(42L)).thenReturn(Optional.empty());
        when(auditLogRepository.saveAndFlush(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog log = invocation.getArgument(0);
            log.setId(100L);
            return log;
        });

        auditLogService.log(requestUser, "LOGIN_SUCCESS", "127.0.0.1", "JUnit", true, "ok");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).saveAndFlush(captor.capture());

        AuditLog saved = captor.getValue();
        assertNull(saved.getUser());
        assertEquals("patient@example.com", saved.getUserEmail());
        assertEquals("kc-42", saved.getKeycloakId());
        assertEquals("LOGIN_SUCCESS", saved.getAction());
        assertEquals("127.0.0.1", saved.getIpAddress());
    }

    @Test
    void linksAuditRowToManagedUserWhenAvailable() {
        Patient requestUser = new Patient();
        requestUser.setId(42L);
        requestUser.setEmail("patient@example.com");
        requestUser.setKeycloakId("kc-42");

        Patient managedUser = new Patient();
        managedUser.setId(42L);
        managedUser.setEmail("patient@example.com");
        managedUser.setKeycloakId("kc-42");

        when(appUserRepository.findById(42L)).thenReturn(Optional.of(managedUser));
        when(auditLogRepository.saveAndFlush(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog log = invocation.getArgument(0);
            log.setId(101L);
            return log;
        });

        auditLogService.log(requestUser, "LOGIN_SUCCESS", "127.0.0.1", "JUnit", true, "ok");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).saveAndFlush(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals(42L, saved.getUser().getId());
        assertEquals("patient@example.com", saved.getUserEmail());
        assertEquals("kc-42", saved.getKeycloakId());
    }
}
