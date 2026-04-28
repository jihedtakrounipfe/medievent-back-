package skylinkers.tn.mediconnectbackend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skylinkers.tn.mediconnectbackend.entities.VerificationCode;
import skylinkers.tn.mediconnectbackend.repository.VerificationCodeRepository;
import skylinkers.tn.mediconnectbackend.service.VerificationCodeService;
import skylinkers.tn.mediconnectbackend.utils.EmailService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceTest {

    @Mock
    private VerificationCodeRepository repository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private VerificationCodeService service;

    @Captor
    private ArgumentCaptor<VerificationCode> verificationCodeCaptor;

    @Test
    void generateAndSend_normalizes_email_invalidates_previous_and_sends_email() {
        VerificationCode existing1 = new VerificationCode();
        existing1.setEmail("user@example.com");
        existing1.setCode("111111");

        VerificationCode existing2 = new VerificationCode();
        existing2.setEmail("user@example.com");
        existing2.setCode("222222");

        when(repository.countByEmailAndCreatedAtAfter(eq("user@example.com"), any(LocalDateTime.class))).thenReturn(0L);
        when(repository.findByEmailAndUsedFalse("user@example.com")).thenReturn(List.of(existing1, existing2));
        when(repository.save(any(VerificationCode.class))).thenAnswer(inv -> inv.getArgument(0));

        service.generateAndSend("  USER@EXAMPLE.COM  ", "Amine");

        assertThat(existing1.isUsed()).isTrue();
        assertThat(existing2.isUsed()).isTrue();

        verify(repository).save(verificationCodeCaptor.capture());
        VerificationCode saved = verificationCodeCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getCode()).matches("^[0-9]{6}$");
        assertThat(saved.getExpiresAt()).isNotNull();

        verify(emailService).sendVerificationCode(eq("user@example.com"), eq("Amine"), eq(saved.getCode()));
    }

    @Test
    void generateAndSend_rate_limit_throws() {
        when(repository.countByEmailAndCreatedAtAfter(eq("user@example.com"), any(LocalDateTime.class))).thenReturn(3L);

        assertThatThrownBy(() -> service.generateAndSend("user@example.com", "A"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Resend limit exceeded");

        verify(repository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void verifyCode_valid_marks_used_and_returns_true() {
        VerificationCode vc = new VerificationCode();
        vc.setEmail("user@example.com");
        vc.setCode("123456");
        vc.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(repository.findByEmailAndCodeAndUsedFalseAndExpiresAtAfter(eq("user@example.com"), eq("123456"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(vc));

        boolean ok = service.verifyCode("USER@EXAMPLE.COM", "123456");

        assertThat(ok).isTrue();
        assertThat(vc.isUsed()).isTrue();
    }

    @Test
    void verifyCode_invalid_returns_false() {
        when(repository.findByEmailAndCodeAndUsedFalseAndExpiresAtAfter(any(), any(), any())).thenReturn(Optional.empty());

        boolean ok = service.verifyCode("user@example.com", "000000");

        assertThat(ok).isFalse();
    }

    @Test
    void cleanupExpired_deletes_old_codes() {
        service.cleanupExpired();
        verify(repository).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }
}

