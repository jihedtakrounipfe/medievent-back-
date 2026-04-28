package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.CreateCheckoutRequestDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.CreateCheckoutResponseDTO;

public interface PaymentService {
    CreateCheckoutResponseDTO createCheckoutSession(CreateCheckoutRequestDTO request);
    void handlePaymentSuccess(String stripeSessionId);
    void handlePaymentFailed(String stripeSessionId);
    void handlePaymentSuccessByEmail(String email);
    void handlePaymentFailedByEmail(String email);
}