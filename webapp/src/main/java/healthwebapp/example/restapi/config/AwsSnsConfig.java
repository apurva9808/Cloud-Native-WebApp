package healthwebapp.example.restapi.config;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsSnsConfig {

    // Fetch the AWS region from application properties or environment variables
    @Value("${aws.region:us-east-1}") // Default to "us-east-1" if not set
    private String awsRegion;

    @Bean
    public AmazonSNS snsClient() {
        if (awsRegion == null || awsRegion.trim().isEmpty()) {
            throw new IllegalStateException("AWS region is not configured or is empty");
        }

        // Create and return the AmazonSNS client
        return AmazonSNSClientBuilder.standard()
                .withRegion(awsRegion) // Dynamically sets region
                .withCredentials(new DefaultAWSCredentialsProviderChain()) // Automatically fetches credentials
                .build();
    }
}
