package my.sub.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sub-config")
public class SubConfig {

    private String configPath;

    private String defaultProxyName;

    private String charset;
}
