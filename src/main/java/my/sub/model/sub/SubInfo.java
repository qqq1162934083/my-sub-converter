package my.sub.model.sub;

import lombok.Data;

import java.util.List;

@Data
public class SubInfo {
    private String name;
    private SubType subType;
    private String mainProxyGroupName;
    private String defaultProxyName;
    private List<String> deliveryHeaderList;
    private String subUrl;
    private List<String> includeFileList;
}
