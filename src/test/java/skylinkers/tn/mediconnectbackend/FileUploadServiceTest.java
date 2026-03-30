package skylinkers.tn.mediconnectbackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import skylinkers.tn.mediconnectbackend.exception.FileUploadException;
import skylinkers.tn.mediconnectbackend.service.FileUploadService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "mediconnect.upload.image-path=${java.io.tmpdir}/mediconnect-test-images"
})
class FileUploadServiceTest {

    @Autowired
    private FileUploadService uploadService;

    @Test
    void rejectsNonImageContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "hello".getBytes()
        );
        assertThrows(FileUploadException.class, () -> uploadService.uploadImage(file, 1L));
    }

    @Test
    void acceptsPngWithValidHeader() {
        byte[] pngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                pngHeader
        );
        var res = uploadService.uploadImage(file, 1L);
        assertNotNull(res);
        assertTrue(res.url().startsWith("/api/files/images/"));
    }
}

