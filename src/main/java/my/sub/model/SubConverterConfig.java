package my.sub.model;

import lombok.Data;

@Data
public class SubConverterConfig extends ExpandoJsonObject {
    private String host;
    private String port;
}