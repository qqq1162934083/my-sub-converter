package my.sub.config;

import my.sub.model.SubConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataConfig {
    @Bean
    @ConfigurationProperties(prefix = "config")
    public SubConfig subConfig() {
        return new SubConfig();
    }
}
