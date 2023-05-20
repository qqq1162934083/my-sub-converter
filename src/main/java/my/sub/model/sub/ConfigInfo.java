package my.sub.model.sub;

import lombok.Data;

import java.util.List;

@Data
public class ConfigInfo {
    private String name;
    private String cliType;
    private List<String> includeConfigList;
    private List<String> includeSubList;
}
