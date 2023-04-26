package my.sub.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class SubConfig {
    private String url;
    private String type;
    @JsonProperty("header-names")
    private Set<String> headerNames;
    @JsonProperty("main-select-name")
    private List<Map<String,String>> mainSelectName;
}
