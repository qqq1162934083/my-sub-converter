package my.sub.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RequestSubServerInput {
    private String url;
    private Map<String, String> requestHeaders;
    private List<String> deliveryHeaders;
}
