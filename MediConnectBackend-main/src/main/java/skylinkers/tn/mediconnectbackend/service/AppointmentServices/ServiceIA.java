package skylinkers.tn.mediconnectbackend.service.AppointmentServices;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import skylinkers.tn.mediconnectbackend.entities.Appointment;

import java.util.HashMap;
import java.util.Map;

@Service
public class ServiceIA {

    private final RestTemplate restTemplate = new RestTemplate();

    public String predictNoShow(Appointment app, int historyNoShow) {

        String url = "http://localhost:8000/predict";

        Map<String, Object> data = new HashMap<>();
        data.put("day_of_week", app.getDate().getDayOfWeek().getValue());
        data.put("hour", app.getHeure().getHour());
        data.put("is_weekend", app.getDate().getDayOfWeek().getValue() >= 6 ? 1 : 0);
        data.put("patient_history_no_show", historyNoShow);
        data.put("delay_days", 1); // simplification

        ResponseEntity<Map> response = restTemplate.postForEntity(url, data, Map.class);

        return (String) response.getBody().get("prediction");
    }
}