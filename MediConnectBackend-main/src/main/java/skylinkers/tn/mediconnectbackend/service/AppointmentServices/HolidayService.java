package skylinkers.tn.mediconnectbackend.service.AppointmentServices;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HolidayService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Cacheable("holidays")
    public List<LocalDate> getTunisianHolidays(int year) {
        String url = "https://date.nager.at/api/v3/PublicHolidays/" + year + "/TN";

        ResponseEntity<List<Map>> response = restTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map>>() {}
        );

        return response.getBody().stream()
                .map(h -> LocalDate.parse(h.get("date").toString()))
                .collect(Collectors.toList());
    }

    public boolean isHoliday(LocalDate date) {
        return getTunisianHolidays(date.getYear()).contains(date);
    }
}