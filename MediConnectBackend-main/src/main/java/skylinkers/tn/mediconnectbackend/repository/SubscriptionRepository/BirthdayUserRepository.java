package skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import skylinkers.tn.mediconnectbackend.entities.AppUser;

import java.util.List;

@Repository
public interface BirthdayUserRepository extends JpaRepository<AppUser, Long> {

    @Query("SELECT u FROM AppUser u WHERE MONTH(u.dateOfBirth) = :month AND DAY(u.dateOfBirth) = :day")
    List<AppUser> findByBirthMonthAndDay(@Param("month") int month, @Param("day") int day);
}