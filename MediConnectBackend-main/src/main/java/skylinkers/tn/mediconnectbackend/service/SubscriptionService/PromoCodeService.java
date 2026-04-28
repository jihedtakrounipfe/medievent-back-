package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PromoCodeRequestDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PromoCodeResponseDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PromoCodeValidationDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PromoCodeValidationResponseDTO;

import java.util.List;

public interface PromoCodeService {
    // Admin operations
    PromoCodeResponseDTO createPromoCode(PromoCodeRequestDTO request, Long adminId);
    PromoCodeResponseDTO updatePromoCode(Long id, PromoCodeRequestDTO request);
    void deletePromoCode(Long id);
    PromoCodeResponseDTO getPromoCode(Long id);
    List<PromoCodeResponseDTO> getAllPromoCodes();
    void togglePromoCode(Long id);

    // User operations
    PromoCodeValidationResponseDTO validatePromoCode(PromoCodeValidationDTO request);
    void markPromoCodeAsUsed(String code, Long userId, Long subscriptionId);
}