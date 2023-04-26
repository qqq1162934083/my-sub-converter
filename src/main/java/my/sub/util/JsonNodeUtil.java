package my.sub.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.var;

import java.util.*;
import java.util.stream.StreamSupport;

public class JsonNodeUtil {

    /**
     * 判断是否是集合结点
     *
     * @param node
     * @return
     */
    public static boolean isArrayNode(Object node) {
        return node instanceof Iterable || node instanceof Iterator;
    }

    /**
     * 判断是否是对象结点
     *
     * @param node
     * @return
     */
    public static boolean isObjectNode(Object node) {
        return node instanceof Map || node instanceof ObjectNode;
    }

    /**
     * 转map，针对map 和 objectNode类型
     *
     * @param jsonNode
     * @return
     */
    public static <T> Map<String, T> asMap(Object jsonNode, Class<T> clazz) {
        if (!isObjectNode(jsonNode))
            throw new IllegalArgumentException("不是一个对象结点");

        var result = new HashMap<String, T>();
        if (jsonNode instanceof Map) {
            var mapNode = (Map) jsonNode;
            for (var key : mapNode.keySet()) {
                result.put(key.toString(), (T) mapNode.get(key));
            }
        } else {
            var objectNode = (ObjectNode) jsonNode;
            for (var it = objectNode.fields(); it.hasNext(); ) {
                var entry = it.next();
                result.put(entry.getKey(), (T) entry.getValue());
            }
        }
        return result;
    }

    /**
     * 转为list
     *
     * @param jsonNode
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> List<T> asList(Object jsonNode, Class<T> clazz) {
        if (!isArrayNode(jsonNode))
            throw new IllegalArgumentException("不是一个集合结点");

        var result = new ArrayList<T>();
        if (jsonNode instanceof Iterable) {
            for (var item : ((Iterable) jsonNode)) {
                result.add((T) item);
            }
        } else {
            var iterator = (Iterator) jsonNode;
            while (iterator.hasNext()) {
                result.add((T) iterator.next());
            }
        }
        return result;
    }
}
