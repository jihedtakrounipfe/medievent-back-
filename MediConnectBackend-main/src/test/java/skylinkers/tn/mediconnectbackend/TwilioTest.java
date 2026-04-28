package skylinkers.tn.mediconnectbackend;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TwilioTest {

    @Test
    public void testWhatsApp() {
        Twilio.init("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");

        Message message = Message.creator(
                new PhoneNumber("whatsapp:+21699040331"),
                new PhoneNumber("whatsapp:+14155238886"),
                "Bonjour, ceci est un test de MediConnect."
        ).create();

        System.out.println("Message SID: " + message.getSid());
    }
}