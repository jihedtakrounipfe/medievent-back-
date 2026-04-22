package skylinkers.tn.mediconnectbackend.dto;

import lombok.Data;

@Data
public class GuestInviteRequest {
    private String guestEmail;
    private String guestName;
}
