package my.sub.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.var;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonNodeUtil {
    /**
     * 转map，针对objectNode
     *
     * @param jsonNode
     * @return
     */
    public static <T> Map<String, T> asMap(Object jsonNode, Class<T> clazz) {
        var result = new HashMap<String, T>();
        if (jsonNode instanceof Map) {
            var mapNode = (Map) jsonNode;
            for (var key : mapNode.keySet()) {
                result.put(key.toString(), (T) mapNode.get(key));
            }
        }
        if (!(jsonNode instanceof ObjectNode)) {
            throw new IllegalArgumentException("as json node is not a object node or map,convert it to map failed");
        }
        var objectNode = (ObjectNode) jsonNode;
        for (var it = objectNode.fields(); it.hasNext(); ) {
            var entry = it.next();
            result.put(entry.getKey(), (T) entry.getValue());
        }
        return result;
    }

    /**
     * 转为list
     * @param jsonNode
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> List<T> asList(Object jsonNode, Class<T> clazz) {
        var result = new ArrayList<T>();
        if (jsonNode instanceof List) {
            var listNode = (List) jsonNode;
            for (var item : listNode) {
                result.add((T) item);
            }
        }
        if (!(jsonNode instanceof ObjectNode)) {
            throw new IllegalArgumentException("as json node is not a array node or array,convert it to list failed");
        }
        var objectNode = (ObjectNode) jsonNode;
        for (var it = objectNode.fields(); it.hasNext(); ) {
            var entry = it.next();
            result.add((T) entry.getValue());
        }
        return result;
    }
}
