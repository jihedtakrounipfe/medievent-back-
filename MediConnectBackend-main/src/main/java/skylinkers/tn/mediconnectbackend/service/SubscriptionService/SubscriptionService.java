package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.*;
import skylinkers.tn.mediconnectbackend.entities.UserCredit;

import java.util.List;

public interface SubscriptionService {

    SubscriptionResponse subscribe(SubscriptionRequest request);

    SubscriptionResponse getActiveSubscription(Long userId);

    List<SubscriptionResponse> getAllSubscriptions(Long userId);

    SubscriptionResponse cancel(Long userId, CancellationRequestDTO request);

    boolean hasFeature(Long userId, String feature);

    void toggleAutoRenew(Long userId);

    UserCredit getUserCredit(Long userId);
    List<CreditHistoryEntryDTO> getUserCreditHistory(Long userId);

    PriceSummaryResponseDTO calculatePrice(PriceCalculationRequestDTO request);

    CreateCheckoutResponseDTO upgradeDowngrade(Long userId, UpgradeDowngradeRequestDTO request);

}