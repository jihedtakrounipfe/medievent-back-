package skylinkers.tn.mediconnectbackend.service.UserServices.IUser;

import skylinkers.tn.mediconnectbackend.dto.request.CreateAdminRequest;
import skylinkers.tn.mediconnectbackend.dto.response.AppUserResponse;

public interface AdministratorService {
    AppUserResponse registerAdmin(CreateAdminRequest request);
}

