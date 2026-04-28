package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import org.springframework.web.multipart.MultipartFile;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.StudentVerificationRequestDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.StudentVerificationResponseDTO;

import java.util.List;

public interface StudentVerificationService {
    StudentVerificationResponseDTO submitVerification(StudentVerificationRequestDTO request, MultipartFile document);
    StudentVerificationResponseDTO getVerificationStatus(Long userId);
    boolean isApproved(Long userId);
    List<StudentVerificationResponseDTO> getPendingVerifications();
    void processPendingVerifications();
    StudentVerificationResponseDTO getVerificationStatusByKeycloakId(String keycloakId);
    StudentVerificationResponseDTO submitVerificationByKeycloakId(String keycloakId, StudentVerificationRequestDTO request, MultipartFile document);
}