package skylinkers.tn.mediconnectbackend.dto.request;

import skylinkers.tn.mediconnectbackend.entities.enums.Specialization;
import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
import lombok.Data;

/**
 * Query object for cross-entity user search.
 * All fields are optional — null fields are ignored in the Specification.
 */
@Data
public class UserSearchCriteria {

    private String         name;            // searches firstName + lastName
    private String         email;
    private UserType       userType;        // filter by subtype
    private Specialization specialization;  // doctors only
    private String         city;            // extracted from officeAddress / address
    private Boolean        isActive;
    private Boolean        isVerified;      // doctors only
}