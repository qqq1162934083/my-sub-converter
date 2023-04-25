package my.sub.model;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Data
public class SubConfig extends ExpandoJsonObject{
    private String url;
    private String type;
    private SubConverterConfig subConverter;
}
