package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.StudentVerificationRequestDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.StudentVerificationResponseDTO;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.StudentVerification;
import skylinkers.tn.mediconnectbackend.entities.enums.SubVerificationStatus;
import skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository.StudentVerificationRepository;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentVerificationServiceImpl implements StudentVerificationService {

    private final StudentVerificationRepository verificationRepository;
    private final AppUserRepository userRepository;
    private final AzureDocumentService azureDocumentService;
    private final SubscriptionEmailService subscriptionEmailService;

    @Autowired
    @Qualifier("applicationTaskExecutor")
    private AsyncTaskExecutor taskExecutor;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-1.5-flash-latest}")
    private String geminiModel;

    @Value("${azure.storage.connection-string}")
    private String storageConnectionString;

    @Value("${azure.storage.student-verification-container-name:student-verifications}")
    private String studentVerificationContainerName;

    @Value("${student.verification.allow-resubmission:false}")
    private boolean allowResubmission;

    @Value("${student.verification.minimum-review-minutes:3}")
    private long minimumReviewMinutes;

    @Override
    @Transactional
    public StudentVerificationResponseDTO submitVerification(
            StudentVerificationRequestDTO request, MultipartFile document) {

        if (allowResubmission) {
            long deletedRows = verificationRepository.deleteByUserId(request.getUserId());
            if (deletedRows > 0) {
                log.info("Resubmission mode enabled: deleted {} old verification rows for user {}", deletedRows, request.getUserId());
            }
        }

        // Check if user already has a pending/approved verification
        if (verificationRepository.existsByUserIdAndStatus(request.getUserId(), SubVerificationStatus.PENDING)) {
            throw new RuntimeException("You already have a pending verification request");
        }
        if (verificationRepository.existsByUserIdAndStatus(request.getUserId(), SubVerificationStatus.APPROVED)) {
            throw new RuntimeException("You are already verified as a student");
        }

        AppUser user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Upload document to Azure Blob Storage
        String documentUrl = uploadToAzureBlob(document, request.getUserId());

        // Extract text via Azure OCR
        String extractedText = azureDocumentService.extractTextFromDocument(document);

        // Build and SAVE AS PENDING (no validation yet)
        StudentVerification verification = StudentVerification.builder()
                .user(user)
                .fullName(request.getFullName())
                .universityName(request.getUniversityName())
                .studentIdNumber(normalizeOptionalField(request.getStudentIdNumber()))
                .facultyEmail(normalizeOptionalField(request.getFacultyEmail()))
                .documentUrl(documentUrl)
                .documentFileName(document.getOriginalFilename())
                .extractedText(extractedText)
                .status(SubVerificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        verificationRepository.save(verification);

        // Send "received" email
        subscriptionEmailService.sendStudentVerificationReceivedEmail(user.getEmail(), request.getFullName());
        log.info("Student verification PENDING for user: {}", user.getEmail());

        // Process quickly in background so users do not wait for the scheduler tick.
        taskExecutor.execute(() -> {
            try {
                processPendingVerifications();
            } catch (Exception e) {
                log.error("Async verification processing failed after submit: {}", e.getMessage(), e);
            }
        });

        return mapToDTO(verification);
    }

    @Override
    public StudentVerificationResponseDTO getVerificationStatus(Long userId) {
        return verificationRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .map(this::mapToDTO)
                .orElseGet(this::emptyStatusDTO);
    }

    @Override
    public boolean isApproved(Long userId) {
        return verificationRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .map(v -> v.getStatus() == SubVerificationStatus.APPROVED)
                .orElse(false);
    }

    @Override
    public List<StudentVerificationResponseDTO> getPendingVerifications() {
        return verificationRepository.findByStatus(SubVerificationStatus.PENDING)
                .stream().map(this::mapToDTO).toList();
    }

    @Override
    public void processPendingVerifications() {
        List<StudentVerification> pendingVerifications = verificationRepository.findByStatus(SubVerificationStatus.PENDING);
        for (StudentVerification verification : pendingVerifications) {
            try {
                if (verification.getCreatedAt() != null) {
                    long elapsedSeconds = java.time.Duration.between(verification.getCreatedAt(), LocalDateTime.now()).getSeconds();
                    long requiredSeconds = minimumReviewMinutes * 60;
                    if (elapsedSeconds < requiredSeconds) {
                        continue;
                    }
                }

                if (verification.getCreatedAt() == null) {
                    continue;
                }

                // Check if retry limit exceeded (3-strike rule for Gemini failures)
                if (verification.getRetryCount() != null && verification.getRetryCount() >= 3) {
                    verification.setStatus(SubVerificationStatus.REJECTED);
                    verification.setRejectionReason("AI verification service temporarily unavailable, please resubmit your documents");
                    verification.setConfidenceScore(0.0);
                    verificationRepository.save(verification);
                    subscriptionEmailService.sendStudentVerificationRejectedEmail(verification.getUser().getEmail(), verification.getFullName(), verification.getRejectionReason());
                    log.warn("Student verification marked REJECTED after 3 retry attempts for user: {}", verification.getUser().getEmail());
                    continue;
                }

                // Validate using Gemini
                ValidationResult validation = validateStudentDocument(
                    verification.getExtractedText(),
                    verification.getFullName(),
                    verification.getUniversityName(),
                    verification.getStudentIdNumber(),
                    verification.getFacultyEmail()
                );

                if (validation.isValid()) {
                    verification.setVerifiedAt(LocalDateTime.now());
                    verification.setStatus(SubVerificationStatus.APPROVED);
                    verification.setConfidenceScore(validation.score());
                    verification.setRetryCount(0);
                    verificationRepository.save(verification);
                    subscriptionEmailService.sendStudentVerificationApprovedEmail(verification.getUser().getEmail(), verification.getFullName());
                    log.info("Student verification APPROVED for user: {}", verification.getUser().getEmail());
                } else {
                    verification.setStatus(SubVerificationStatus.REJECTED);
                    verification.setRejectionReason(validation.reason());
                    verification.setConfidenceScore(validation.score());
                    verification.setRetryCount(0);
                    verificationRepository.save(verification);
                    subscriptionEmailService.sendStudentVerificationRejectedEmail(verification.getUser().getEmail(), verification.getFullName(), validation.reason());
                    log.info("Student verification REJECTED for user: {} — {}", verification.getUser().getEmail(), validation.reason());
                }
            } catch (HttpStatusCodeException e) {
                // AI-only: any Gemini HTTP error increments retry count
                int currentRetry = verification.getRetryCount() != null ? verification.getRetryCount() : 0;
                verification.setRetryCount(currentRetry + 1);
                verificationRepository.save(verification);
                log.warn("Gemini HTTP {} error for user {}. Retry count: {}/3", e.getStatusCode(), verification.getUser().getId(), verification.getRetryCount());
            } catch (Exception e) {
                log.error("Error processing verification for user {}: {}", verification.getUser().getId(), e.getMessage());
            }
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String uploadToAzureBlob(MultipartFile file, Long userId) {
        try {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(storageConnectionString)
                    .buildClient();

            BlobContainerClient containerClient = blobServiceClient
                    .getBlobContainerClient(studentVerificationContainerName);

            String blobName = "student-verifications/" + userId + "/" +
                    UUID.randomUUID() + "-" + file.getOriginalFilename();

            BlobClient blobClient = containerClient.getBlobClient(blobName);
            blobClient.upload(file.getInputStream(), file.getSize(), true);

            return blobClient.getBlobUrl();
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload document to Azure: " + e.getMessage());
        }
    }

    private ValidationResult validateStudentDocument(
            String text,
            String fullName,
            String universityName,
            String studentIdNumber,
            String facultyEmail
    ) {
        if (text == null || text.isBlank()) {
            return new ValidationResult(false, "Could not extract text from document", 0.0);
        }

        try {
                // Prepare the prompt for Gemini
                String prompt = "Analyze this student document and determine if it's valid. " +
                    "Checks: 1) confirms student status, 2) contains faculty/university name, " +
                    "3) card validity is for academic year 2025-2026. " +
                    "Student card number is optional if not provided. Faculty email is optional if provided. " +
                    "Respond ONLY as a JSON object (no markdown, no code blocks) with this schema: " +
                    "{\"decision\":\"APPROVED or REJECTED\",\"reason\":\"max 20 words\",\"confidence\":0.75}. " +
                    "Declared full name: " + nullSafe(fullName) + ". " +
                    "Declared university/faculty: " + nullSafe(universityName) + ". " +
                    "Declared student card number: " + nullSafe(studentIdNumber) + ". " +
                    "Declared faculty email: " + nullSafe(facultyEmail) + ". " +
                    "Document text:\n" + text;

                // Create Gemini request body
            JsonObject requestBody = new JsonObject();
                JsonArray contents = new JsonArray();
                JsonObject content = new JsonObject();
                JsonArray parts = new JsonArray();
                JsonObject part = new JsonObject();
                part.addProperty("text", prompt);
                parts.add(part);
                content.add("parts", parts);
                contents.add(content);
                requestBody.add("contents", contents);

                JsonObject generationConfig = new JsonObject();
                generationConfig.addProperty("temperature", 0.1);
                generationConfig.addProperty("maxOutputTokens", 512);
                requestBody.add("generationConfig", generationConfig);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

                // Make request
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
                String geminiUrl = "https://generativelanguage.googleapis.com/v1/models/"
                    + geminiModel
                    + ":generateContent?key="
                    + geminiApiKey;

            String response = restTemplate.postForObject(
                    geminiUrl,
                    request,
                    String.class
            );

            // Parse response
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
                if (candidates == null || candidates.isEmpty()) {
                throw new RuntimeException("Gemini returned no candidates");
                }
                String aiResponse = null;
                String jsonPayload = null;
                String truncatedCandidate = null;
                for (JsonElement candidateElement : candidates) {
                    if (!candidateElement.isJsonObject()) {
                        continue;
                    }

                    JsonObject candidate = candidateElement.getAsJsonObject();
                    String finishReason = candidate.has("finishReason") && !candidate.get("finishReason").isJsonNull()
                        ? candidate.get("finishReason").getAsString()
                        : "";
                    if (!candidate.has("content") || candidate.get("content").isJsonNull()) {
                        continue;
                    }

                    JsonObject contentObject = candidate.getAsJsonObject("content");
                    JsonArray responseParts = contentObject.getAsJsonArray("parts");
                    if (responseParts == null || responseParts.isEmpty()) {
                        continue;
                    }

                    StringBuilder aiResponseBuilder = new StringBuilder();
                    for (JsonElement partElement : responseParts) {
                        if (!partElement.isJsonObject()) {
                            continue;
                        }
                        JsonObject responsePart = partElement.getAsJsonObject();
                        if (!responsePart.has("text") || responsePart.get("text").isJsonNull()) {
                            continue;
                        }
                        if (!aiResponseBuilder.isEmpty()) {
                            aiResponseBuilder.append('\n');
                        }
                        aiResponseBuilder.append(responsePart.get("text").getAsString());
                    }

                    String candidateText = aiResponseBuilder.toString().trim();
                    if (candidateText.isBlank()) {
                        continue;
                    }

                    String candidateJson = extractJsonObject(candidateText);
                    if (candidateJson == null || candidateJson.isBlank() || "null".equalsIgnoreCase(candidateJson)) {
                        if ("MAX_TOKENS".equalsIgnoreCase(finishReason)) {
                            truncatedCandidate = candidateText;
                        }
                        continue;
                    }

                    aiResponse = candidateText;
                    jsonPayload = candidateJson;
                    break;
                }

                log.info("Gemini Response: {}", abbreviate(aiResponse, 1000));

            if (jsonPayload == null || jsonPayload.isBlank()) {
                ValidationResult truncatedResult = recoverFromTruncatedCandidate(truncatedCandidate);
                if (truncatedResult != null) {
                    return truncatedResult;
                }
                throw new RuntimeException("Gemini returned no JSON object from any candidate. Raw API response: " + abbreviate(response, 2000));
            }

            // Parse strict JSON decision from Gemini
            JsonElement parsedElement = JsonParser.parseString(jsonPayload);
            if (!parsedElement.isJsonObject()) {
                throw new RuntimeException("Gemini response is not a JSON object: " + abbreviate(jsonPayload, 500));
            }
            JsonObject parsed = parsedElement.getAsJsonObject();
            String decision = parsed.has("decision") && !parsed.get("decision").isJsonNull()
                    ? parsed.get("decision").getAsString().trim().toUpperCase(Locale.ROOT)
                    : "";
            String reason = parsed.has("reason") && !parsed.get("reason").isJsonNull()
                    ? parsed.get("reason").getAsString().trim()
                    : "No reason provided";
            double score = parsed.has("confidence") && !parsed.get("confidence").isJsonNull()
                    ? parsed.get("confidence").getAsDouble()
                    : 0.0;

            if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) {
                throw new RuntimeException("Gemini returned invalid decision: " + decision);
            }

            if (score < 0.0) {
                score = 0.0;
            } else if (score > 1.0) {
                score = 1.0;
            }

            boolean isApproved = "APPROVED".equals(decision);
            return new ValidationResult(isApproved, isApproved ? null : reason, score);

        } catch (HttpStatusCodeException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("Gemini HTTP error. status={} body={}", e.getStatusCode(), abbreviate(responseBody, 1000));
            if (e.getStatusCode().value() == 429) {
                throw new GeminiQuotaExceededException("Gemini quota exceeded", e);
            }
            throw new RuntimeException("Gemini validation unavailable: HTTP " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Gemini call failed: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini validation unavailable", e);
        }
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private String extractJsonObject(String response) {
        if (response == null) {
            return null;
        }

        String cleaned = response
            .replaceAll("(?is)```json\\s*", "")
            .replaceAll("(?is)```\\s*", "")
            .trim();

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start == -1 || end == -1 || start >= end) {
            return null;
        }

        return cleaned.substring(start, end + 1).trim();
    }

    private ValidationResult recoverFromTruncatedCandidate(String candidateText) {
        if (candidateText == null || candidateText.isBlank()) {
            return null;
        }

        Pattern decisionPattern = Pattern.compile("\\\"decision\\\"\\s*:\\s*\\\"(APPROVED|REJECTED)\\\"", Pattern.CASE_INSENSITIVE);
        Matcher decisionMatcher = decisionPattern.matcher(candidateText);
        if (!decisionMatcher.find()) {
            return null;
        }

        String decision = decisionMatcher.group(1).toUpperCase(Locale.ROOT);
        Pattern reasonPattern = Pattern.compile("\\\"reason\\\"\\s*:\\s*\\\"([^\\\"]*)", Pattern.CASE_INSENSITIVE);
        Matcher reasonMatcher = reasonPattern.matcher(candidateText);
        String reason = reasonMatcher.find()
            ? reasonMatcher.group(1).trim()
            : "AI response was truncated";

        log.warn("Gemini response truncated (MAX_TOKENS). Recovering decision from partial JSON: {}", abbreviate(candidateText, 300));
        boolean approved = "APPROVED".equals(decision);
        double recoveredScore = 0.6;
        return new ValidationResult(approved, approved ? null : reason, recoveredScore);
    }

    private StudentVerificationResponseDTO mapToDTO(StudentVerification v) {
        SubVerificationStatus status = v.getStatus();
        Long userId = v.getUser() != null ? v.getUser().getId() : null;
        return StudentVerificationResponseDTO.builder()
                .id(v.getId())
            .userId(userId)
                .requestFound(true)
                .fullName(v.getFullName())
                .universityName(v.getUniversityName())
                .studentIdNumber(v.getStudentIdNumber())
                .facultyEmail(v.getFacultyEmail())
                .status(status)
                .progressPercentage(computeProgressPercentage(v))
                .progressStep(computeProgressStep(v))
                .progressMessage(computeProgressMessage(v))
            .nextAction(computeNextAction(status))
            .nextPagePath(computeNextPagePath(status, userId))
                .rejectionReason(v.getRejectionReason())
                .confidenceScore(v.getConfidenceScore())
                .createdAt(v.getCreatedAt())
                .verifiedAt(v.getVerifiedAt())
                .expiresAt(v.getExpiresAt())
                .build();
    }

    private StudentVerificationResponseDTO emptyStatusDTO() {
        return StudentVerificationResponseDTO.builder()
                .requestFound(false)
                .status(null)
                .progressPercentage(0)
                .progressStep("NOT_SUBMITTED")
                .progressMessage("No verification request submitted yet")
                .nextAction("SHOW_UPLOAD_FORM")
                .nextPagePath("/checkout")
                .build();
    }

    private Integer computeProgressPercentage(StudentVerification verification) {
        SubVerificationStatus status = verification.getStatus();
        if (status == null) {
            return 0;
        }
        if (status == SubVerificationStatus.PENDING) {
            return switch (computePendingCheckpoint(verification)) {
                case DOCUMENT_RECEIVED -> 20;
                case OCR_EXTRACTION -> 40;
                case IDENTITY_MATCHING -> 55;
                case AI_REVIEW -> 75;
                case FINAL_DECISION_QUEUE -> 90;
            };
        }

        return switch (status) {
            case APPROVED, REJECTED, EXPIRED -> 100;
            case PENDING -> 55;
        };
    }

    private String computeProgressStep(StudentVerification verification) {
        SubVerificationStatus status = verification.getStatus();
        if (status == null) {
            return "NOT_SUBMITTED";
        }
        return switch (status) {
            case PENDING -> switch (computePendingCheckpoint(verification)) {
                case DOCUMENT_RECEIVED -> "DOCUMENT_RECEIVED";
                case OCR_EXTRACTION -> "OCR_EXTRACTION";
                case IDENTITY_MATCHING -> "IDENTITY_MATCHING";
                case AI_REVIEW -> "AI_REVIEW";
                case FINAL_DECISION_QUEUE -> "FINAL_DECISION_QUEUE";
            };
            case APPROVED -> "COMPLETED_APPROVED";
            case REJECTED -> "COMPLETED_REJECTED";
            case EXPIRED -> "COMPLETED_EXPIRED";
        };
    }

    private String computeProgressMessage(StudentVerification verification) {
        SubVerificationStatus status = verification.getStatus();
        String rejectionReason = verification.getRejectionReason();
        if (status == null) {
            return "No verification request submitted yet";
        }
        return switch (status) {
            case PENDING -> switch (computePendingCheckpoint(verification)) {
                case DOCUMENT_RECEIVED -> "Document uploaded successfully";
                case OCR_EXTRACTION -> "Extracting text from your document";
                case IDENTITY_MATCHING -> "Matching details with submitted information";
                case AI_REVIEW -> "Running AI verification checks";
                case FINAL_DECISION_QUEUE -> "Final review in progress";
            };
            case APPROVED -> "Verification approved. You can now continue to payment";
            case REJECTED -> (rejectionReason == null || rejectionReason.isBlank())
                    ? "Verification rejected"
                    : rejectionReason;
            case EXPIRED -> "Your verification has expired. Please submit a new request";
        };
    }

    private PendingCheckpoint computePendingCheckpoint(StudentVerification verification) {
        LocalDateTime createdAt = verification.getCreatedAt();
        if (createdAt == null || minimumReviewMinutes <= 0) {
            return PendingCheckpoint.AI_REVIEW;
        }

        long elapsedSeconds = java.time.Duration.between(createdAt, LocalDateTime.now()).getSeconds();
        long totalSeconds = minimumReviewMinutes * 60;

        if (elapsedSeconds < 0) {
            elapsedSeconds = 0;
        }
        if (elapsedSeconds > totalSeconds) {
            elapsedSeconds = totalSeconds;
        }
        if (totalSeconds <= 0) {
            return PendingCheckpoint.AI_REVIEW;
        }

        double ratio = elapsedSeconds / (double) totalSeconds;
        if (ratio < 0.15) {
            return PendingCheckpoint.DOCUMENT_RECEIVED;
        }
        if (ratio < 0.35) {
            return PendingCheckpoint.OCR_EXTRACTION;
        }
        if (ratio < 0.60) {
            return PendingCheckpoint.IDENTITY_MATCHING;
        }
        if (ratio < 0.85) {
            return PendingCheckpoint.AI_REVIEW;
        }
        return PendingCheckpoint.FINAL_DECISION_QUEUE;
    }

    private String computeNextAction(SubVerificationStatus status) {
        if (status == null) {
            return "SHOW_UPLOAD_FORM";
        }
        return switch (status) {
            case PENDING -> "REDIRECT_TO_PENDING_PAGE";
            case APPROVED -> "UNLOCK_PAYMENT";
            case REJECTED -> "SHOW_RETRY_UPLOAD";
            case EXPIRED -> "SHOW_RETRY_UPLOAD";
        };
    }

    private String computeNextPagePath(SubVerificationStatus status, Long userId) {
        String suffix = userId == null ? "" : ("?userId=" + userId);
        if (status == null) {
            return "/checkout" + suffix;
        }
        return switch (status) {
            case PENDING -> "/student-verification-request" + suffix;
            case APPROVED -> "/checkout" + suffix;
            case REJECTED -> "/checkout" + suffix;
            case EXPIRED -> "/checkout" + suffix;
        };
    }

    private String normalizeOptionalField(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "not provided" : value;
    }

    private enum PendingCheckpoint {
        DOCUMENT_RECEIVED,
        OCR_EXTRACTION,
        IDENTITY_MATCHING,
        AI_REVIEW,
        FINAL_DECISION_QUEUE
    }

    private static class GeminiQuotaExceededException extends RuntimeException {
        GeminiQuotaExceededException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private record ValidationResult(boolean isValid, String reason, double score) {}




    @Override
    public StudentVerificationResponseDTO getVerificationStatusByKeycloakId(String keycloakId) {
        AppUser user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return verificationRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .map(this::mapToDTO)
                .orElseGet(this::emptyStatusDTO);
    }

    @Override
    @Transactional
    public StudentVerificationResponseDTO submitVerificationByKeycloakId(
            String keycloakId, StudentVerificationRequestDTO request, MultipartFile document) {
        AppUser user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        // reuse existing logic by setting userId on the request
        request.setUserId(user.getId());
        return submitVerification(request, document);
    }
}