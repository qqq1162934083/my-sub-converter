package my.sub.model;

import lombok.Data;

import java.util.Map;

@Data
public class RequestSubServerInfoResult {
    private String body;
    private Map<String, Object> deliveryHeaders;

    public RequestSubServerInfoResult(String body, Map<String, Object> deliveryHeaders) {
        setBody(body);
        setDeliveryHeaders(deliveryHeaders);
    }
}
