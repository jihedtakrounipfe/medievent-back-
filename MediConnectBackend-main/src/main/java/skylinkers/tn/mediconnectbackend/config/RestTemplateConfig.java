package skylinkers.tn.mediconnectbackend.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(RiskScoringProperties.class)
public class RestTemplateConfig {

    @Bean
    @Qualifier("riskScoringRestTemplate")
    public RestTemplate riskScoringRestTemplate(RiskScoringProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.timeoutMs());
        factory.setReadTimeout(props.timeoutMs());
        return new RestTemplate(factory);
    }
}
