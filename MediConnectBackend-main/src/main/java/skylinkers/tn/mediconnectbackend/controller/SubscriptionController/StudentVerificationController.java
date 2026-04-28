package skylinkers.tn.mediconnectbackend.controller.SubscriptionController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.StudentVerificationRequestDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.StudentVerificationResponseDTO;
import skylinkers.tn.mediconnectbackend.service.SubscriptionService.StudentVerificationService;

import java.util.List;

@RestController
@RequestMapping("/api/student-verification")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class StudentVerificationController {

    private final StudentVerificationService studentVerificationService;

    // Student submits verification request + document
    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentVerificationResponseDTO> submitVerification(
            Authentication authentication,
            @RequestPart("data") StudentVerificationRequestDTO request,
            @RequestPart("document") MultipartFile document) {
        String keycloakId = authentication.getName();
        log.info("Student verification submission for keycloak user: {}", keycloakId);
        StudentVerificationResponseDTO response = studentVerificationService.submitVerificationByKeycloakId(keycloakId, request, document);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Frontend-Redirect", response.getNextPagePath())
                .body(response);
    }

    // Student checks their own verification status (JWT-based)
    @GetMapping("/status")
    public ResponseEntity<StudentVerificationResponseDTO> getMyStatus(Authentication authentication) {
        String keycloakId = authentication.getName();
        log.info("Student verification status check for keycloak user: {}", keycloakId);
        StudentVerificationResponseDTO status = studentVerificationService.getVerificationStatusByKeycloakId(keycloakId);
        if (!status.isRequestFound()) {
            return ResponseEntity.ok(status); // return 200 with requestFound=false, not 404
        }
        return ResponseEntity.ok(status);
    }

    // Admin — get all pending verifications
    @GetMapping("/pending")
    public ResponseEntity<List<StudentVerificationResponseDTO>> getPending() {
        return ResponseEntity.ok(studentVerificationService.getPendingVerifications());
    }
}