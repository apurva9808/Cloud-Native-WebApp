package healthwebapp.example.restapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

@Configuration
public class MetricsConfig {
    @Bean
    public StatsDClient statsDClient() {
        return new NonBlockingStatsDClient(
                "csye6225", // prefix for application
                "localhost",
                8125
        );
    }
}
