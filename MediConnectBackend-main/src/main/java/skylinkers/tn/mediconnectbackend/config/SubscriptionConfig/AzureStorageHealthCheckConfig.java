package skylinkers.tn.mediconnectbackend.config.SubscriptionConfig;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class AzureStorageHealthCheckConfig {

	@Bean
	CommandLineRunner azureStorageCheck(
			@Value("${azure.storage.connection-string}") String connectionString,
			@Value("${azure.storage.container-name}") String invoiceContainerName,
			@Value("${azure.storage.student-verification-container-name:student-verifications}") String studentVerificationContainerName) {
		return args -> {
			ensureContainerExists(connectionString, invoiceContainerName, "invoice");
			ensureContainerExists(connectionString, studentVerificationContainerName, "student-verification");
		};
	}

	private void ensureContainerExists(String connectionString, String containerName, String purpose) {
		BlobContainerClient containerClient = new BlobContainerClientBuilder()
				.connectionString(connectionString)
				.containerName(containerName)
				.buildClient();

		if (!containerClient.exists()) {
			containerClient.create();
			log.info("Azure Blob connected. {} container created: {}", purpose, containerName);
		} else {
			log.info("Azure Blob connected. {} container exists: {}", purpose, containerName);
		}
	}
}
