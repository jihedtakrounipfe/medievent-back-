package skylinkers.tn.mediconnectbackend.seeder.SubscriptionSeeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import skylinkers.tn.mediconnectbackend.entities.DoctorPlan;
import skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository.DoctorPlanRepository;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DoctorPlanSeeder implements CommandLineRunner {

    private final DoctorPlanRepository doctorPlanRepository;

    @Override
    public void run(String... args) throws Exception {
        seedDoctorPlans();
    }

    private void seedDoctorPlans() {

        // SILVER Plan
        createPlanIfNotExists(
            "SILVER",
            new BigDecimal("30.00"),
            new BigDecimal("300.00"),
            20,    // 20 consultations/month
            true,  // hasCalendarSync
            true,  // hasSearchVisibility
            true,  // hasBasicAnalytics
            true,  // hasAdvancedAnalytics
            true,  // hasForumBadge
            true,  // hasConsultationPrerequisites
            true   // hasAI
        );

        // GOLD Plan
        createPlanIfNotExists(
            "GOLD",
            new BigDecimal("40.00"),
            new BigDecimal("400.00"),
            null,  // unlimited consultations
            true,  // hasCalendarSync
            true,  // hasSearchVisibility
            true,  // hasBasicAnalytics
            true,  // hasAdvancedAnalytics
            true,  // hasForumBadge
            true,  // hasConsultationPrerequisites
            true   // hasAI
        );

        log.info("✅ Doctor plans seeding completed");
    }

    private void createPlanIfNotExists(
            String name,
            BigDecimal priceMonthly,
            BigDecimal priceYearly,
            Integer maxConsultationsPerMonth,
            Boolean hasCalendarSync,
            Boolean hasSearchVisibility,
            Boolean hasBasicAnalytics,
            Boolean hasAdvancedAnalytics,
            Boolean hasForumBadge,
            Boolean hasConsultationPrerequisites,
            Boolean hasAI) {

        if (doctorPlanRepository.existsByNameIgnoreCase(name)) {
            log.debug("Doctor plan {} already exists, skipping", name);
            return;
        }

        DoctorPlan plan = DoctorPlan.builder()
                .name(name)
                .priceMonthly(priceMonthly)
                .priceYearly(priceYearly)
                .maxConsultationsPerMonth(maxConsultationsPerMonth)
                .hasCalendarSync(hasCalendarSync)
                .hasSearchVisibility(hasSearchVisibility)
                .hasBasicAnalytics(hasBasicAnalytics)
                .hasAdvancedAnalytics(hasAdvancedAnalytics)
                .hasForumBadge(hasForumBadge)
                .hasConsultationPrerequisites(hasConsultationPrerequisites)
                .hasAI(hasAI)
                .isActive(true)
                .build();

        doctorPlanRepository.save(plan);
        log.info("✅ Created doctor plan: {}", name);
    }
}
