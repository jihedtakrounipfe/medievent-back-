package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClientBuilder;
import com.azure.ai.formrecognizer.documentanalysis.models.*;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class AzureDocumentService {

    @Value("${azure.document-intelligence.key}")
    private String apiKey;

    @Value("${azure.document-intelligence.endpoint}")
    private String endpoint;

    public String extractTextFromDocument(MultipartFile file) {
        try {
            DocumentAnalysisClient client = new DocumentAnalysisClientBuilder()
                    .credential(new AzureKeyCredential(apiKey))
                    .endpoint(endpoint)
                    .buildClient();

            BinaryData binaryData = BinaryData.fromBytes(file.getBytes());

            AnalyzeDocumentOptions options = new AnalyzeDocumentOptions();

            SyncPoller<OperationResult, AnalyzeResult> poller =
                    client.beginAnalyzeDocument("prebuilt-read", binaryData);

            AnalyzeResult result = poller.getFinalResult();

            StringBuilder extractedText = new StringBuilder();
            for (DocumentPage page : result.getPages()) {
                for (DocumentLine line : page.getLines()) {
                    extractedText.append(line.getContent()).append("\n");
                }
            }

            log.info("Azure OCR extracted {} characters", extractedText.length());
            return extractedText.toString();

        } catch (Exception e) {
            log.error("Azure OCR failed: {}", e.getMessage());
            throw new RuntimeException("Failed to extract text from document: " + e.getMessage());
        }
    }
}