package my.sub.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SubInfo {
    private String name;
    private List<String> deliveryHeaders;
    private String subUrl;
    private Map<String,String> requestHeaders;
    private String defaultProxyName;
    private String mainProxyGroupName;
}
