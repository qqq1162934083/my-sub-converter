package my.sub.model.sub;

import lombok.Data;

import java.util.List;

@Data
public class UserConfig {
    private List<ConfigInfo> configInfoList;
    private String subConverterUrl;
    private List<SubInfo> subInfoList;
    private String charset = "UTF-8";
}
