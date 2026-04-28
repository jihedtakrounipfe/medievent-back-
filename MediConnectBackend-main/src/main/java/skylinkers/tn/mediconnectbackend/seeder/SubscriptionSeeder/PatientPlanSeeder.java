package skylinkers.tn.mediconnectbackend.seeder.SubscriptionSeeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import skylinkers.tn.mediconnectbackend.entities.PatientPlan;
import skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository.PatientPlanRepository;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class PatientPlanSeeder implements CommandLineRunner {

    private final PatientPlanRepository patientPlanRepository;

    @Override
    public void run(String... args) throws Exception {
        seedPatientPlans();
    }

    private void seedPatientPlans() {
        // BASIC Plan
        createPlanIfNotExists(
            "BASIC",
            new BigDecimal("5.00"),
            new BigDecimal("50.00"),
            10,    // 10 appointments/month
            true,  // hasDocumentUpload
            true,  // hasMedicationReminder
            false, // hasLabResults
            false, // hasSelfTestReadings
            true,  // hasForum
            false, // hasAI
            false  // hasHealthEvents
        );

        // PREMIUM Plan (student discount plan)
        createPlanIfNotExists(
            "PREMIUM",
            new BigDecimal("10.00"),
            new BigDecimal("100.00"),
            null,  // unlimited appointments
            true,  // hasDocumentUpload
            true,  // hasMedicationReminder
            true,  // hasLabResults
            true,  // hasSelfTestReadings
            true,  // hasForum
            true,  // hasAI
            true   // hasHealthEvents
        );


        log.info("✅ Patient plans seeding completed");
    }

    private void createPlanIfNotExists(
            String name,
            BigDecimal priceMonthly,
            BigDecimal priceYearly,
            Integer maxAppointmentsPerMonth,
            Boolean hasDocumentUpload,
            Boolean hasMedicationReminder,
            Boolean hasLabResults,
            Boolean hasSelfTestReadings,
            Boolean hasForum,
            Boolean hasAI,
            Boolean hasHealthEvents) {

        if (patientPlanRepository.existsByNameIgnoreCase(name)) {
            log.debug("Patient plan {} already exists, skipping", name);
            return;
        }

        PatientPlan plan = PatientPlan.builder()
                .name(name)
                .priceMonthly(priceMonthly)
                .priceYearly(priceYearly)
                .maxAppointmentsPerMonth(maxAppointmentsPerMonth)
                .hasDocumentUpload(hasDocumentUpload)
                .hasMedicationReminder(hasMedicationReminder)
                .hasLabResults(hasLabResults)
                .hasSelfTestReadings(hasSelfTestReadings)
                .hasForum(hasForum)
                .hasAI(hasAI)
                .hasHealthEvents(hasHealthEvents)
                .isActive(true)
                .isPromoApplicable(true)
                .build();

        patientPlanRepository.save(plan);
        log.info("✅ Created patient plan: {}", name);
    }
}
