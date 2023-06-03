package my.sub.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoleilu.hutool.json.JSONUtil;
import lombok.SneakyThrows;
import lombok.var;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class YamlUtil {
    private enum YamlSingleton {
        INSTANCE;
        private Yaml _instance;

        private YamlSingleton() {
            _instance = createYamlInstance();
        }
    }

    public static Yaml getYaml() {
        return YamlSingleton.INSTANCE._instance;
    }

    public static Yaml createYamlInstance() {
        var loadOption = new LoaderOptions();
        loadOption.setEnumCaseSensitive(false);
        var ctor = new Constructor(loadOption);
        var propUtilsImpl = new PropertyUtils() {

            @Override
            public Property getProperty(Class<?> type, String name) {
                var fieldName = camelize(name);
                return super.getProperty(type, fieldName);
            }

            private String camelize(String input) {
                for (int i = 0; i < input.length(); i++) {
                    if (input.substring(i, i + 1).equals("-")) {
                        input.replace("-", "");
                        input = input.substring(0, i) + input.substring(i + 1, i + 2).toUpperCase() + input.substring(i + 2);
                    }
                    if (input.substring(i, i + 1).equals(" ")) {
                        input.replace(" ", "");
                        input = input.substring(0, i) + input.substring(i + 1, i + 2).toUpperCase() + input.substring(i + 2);
                    }
                }
                return input;
            }
        };
        propUtilsImpl.setSkipMissingProperties(true);
        ctor.setPropertyUtils(propUtilsImpl);
        return new Yaml(ctor);
    }

    /**
     * 反序列化
     *
     * @param yaml
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T deserialize(String yaml, Class<T> clazz) {
        return YamlUtil.getYaml().loadAs(yaml, clazz);
    }

    public static String serialize(Object obj) {
        return YamlUtil.getYaml().dump(obj);
    }

    /**
     * 从yaml的map中获取list
     *
     * @param map
     * @param key
     * @return
     */
    public static List<Object> getList(Object map, String key) {
        return (List<Object>) getObject(map, key);
    }

    public static Object getObject(Object map, String key) {
        return ((Map<String, Object>) map).get(key);
    }

    public static List<Map<String, Object>> getMapList(Object map, String key) {
        return (List<Map<String, Object>>) getObject(map, key);
    }


    /**
     * 从yaml的map中获取map
     *
     * @param node
     * @param key
     * @return
     */
    public static Map<String, Object> getMap(Object node, String key) {
        return (Map<String, Object>) ((Map<String, Object>) node).get(key);
    }

    /**
     * 从yaml的map中获取string
     *
     * @param map
     * @param key
     * @return
     */
    public static String getValue(Object map, String key) {
        var mapValue = (Map<String, Object>) map;
        var value = mapValue.get(key);
        if (!(value instanceof String))
            throw new IllegalArgumentException("key '" + key + "' is not a string");
        return (String) value;
    }

    public static <T> T parse(Object node, TypeReference<T> typeReference) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchFieldException {
        return (T) bindModel(node, typeReference.getType());
    }

    private static boolean isListModel(Type type) {
        return List.class.equals(getClassFromType(type));
    }

    private static boolean isMapModel(Type type) {
        return Map.class.equals(getClassFromType(type));
    }

    private static boolean isSimpleModel(Type type) {
        if (type instanceof ParameterizedType) return false;
        var clazz = (Class<?>) type;
        if (String.class.equals(clazz) ||
                Character.class.equals(clazz) ||
                char.class.equals(clazz) ||
                Boolean.class.equals(clazz) ||
                boolean.class.equals(clazz) ||
                Long.class.equals(clazz) ||
                long.class.equals(clazz) ||
                Integer.class.equals(clazz) ||
                int.class.equals(clazz) ||
                Short.class.equals(clazz) ||
                short.class.equals(clazz) ||
                Float.class.equals(clazz) ||
                float.class.equals(clazz) ||
                BigDecimal.class.equals(clazz) ||
                Double.class.equals(clazz) ||
                double.class.equals(clazz) ||
                Byte.class.equals(clazz) ||
                byte.class.equals(clazz))
            return true;
        return false;
    }


    private static <T> T bindModel(Object map, Type type) throws InstantiationException, IllegalAccessException, NoSuchFieldException {
        return bindModel(map, type, "$root");
    }

    @SneakyThrows
    private static <T> T bindModel(Object node, Type type, String bindKey) throws InstantiationException, IllegalAccessException, NoSuchFieldException {
        System.out.println(bindKey);
        var result = (T) null;
        if (isSimpleModel(type)) {
            result = bindSimpleModel(node, type, bindKey);
        } else {
            result = bindComplexModel(node, type, bindKey);
        }
        System.out.println(bindKey + " : \r\n" + new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result));
        return result;
    }

    private static <T> T bindComplexModel(Object node, Type type, String bindKey) throws NoSuchFieldException, InstantiationException, IllegalAccessException {
        if (isListModel(type)) {
            return bindListModel(node, type, bindKey);
        } else if (isMapModel(type)) {
            return bindMapModel(node, type, bindKey);
        } else {
            return bindBeanModel(node, type, bindKey);
        }
    }

    private static <T> T bindListModel(Object listNode, Type type, String bindKey) throws InstantiationException, IllegalAccessException, NoSuchFieldException {
        if (listNode == null) return null;
        var listNodeValue = (List) listNode;
        var listModel = newInstance(type);
        var list = (List) listModel;
        for (var i = 0; i < listNodeValue.size(); i++) {
            var node = listNodeValue.get(i);
            var itemType = ((ParameterizedType) type).getActualTypeArguments()[0];
            list.add(bindModel(node, itemType, bindKey + "[" + i + "]"));
        }
        return (T) list;
    }

    private static <T> T bindMapModel(Object node, Type type, String bindKey) throws InstantiationException, IllegalAccessException, NoSuchFieldException {
        var map = (Map<String, Object>) node;
        var model = newInstance(type);
        var mapModel = (Map) model;
        var mapKeys = map.keySet().toArray();
        for (var i = 0; i < mapKeys.length; i++) {
            var key = mapKeys[i];
            var value = map.get(key);
            var args = ((ParameterizedType) type).getActualTypeArguments();
            var newMapKey = bindModel(key, args[0], bindKey + "$Map$[" + i + "]$Key");
            var newMapValue = bindModel(value, args[1], bindKey + "$Map$[" + i + "]$Value");
            mapModel.put(newMapKey, newMapValue);
        }
        return (T) model;
    }

    private static <T> T bindBeanModel(Object node, Type type, String bindKey) throws NoSuchFieldException, InstantiationException, IllegalAccessException {
        var map = (Map<String, Object>) node;
        var model = newInstance(type);
        bindProperties(map, type, model, bindKey);
        return (T) model;
    }

    private static <T> T bindSimpleModel(Object value, Type type, String bindKey) {
        var clazz = (Class<T>) type;
        if (value == null) {
            if (char.class.equals(clazz) ||
                    boolean.class.equals(clazz) ||
                    long.class.equals(clazz) ||
                    int.class.equals(clazz) ||
                    short.class.equals(clazz) ||
                    float.class.equals(clazz) ||
                    double.class.equals(clazz) ||
                    byte.class.equals(clazz))
                throw new IllegalArgumentException("key '" + bindKey + "' is not a string");
        }

        var valStr = value.toString();
        if (String.class.equals(clazz)) {
            return (T) valStr;
        } else if (Character.class.equals(clazz) || char.class.equals(clazz)) {
            if (valStr.length() != 1)
                throw new IllegalArgumentException("key '" + bindKey + "' is not a char");
            return (T) Character.valueOf(valStr.charAt(0));
        } else if (Boolean.class.equals(clazz) || boolean.class.equals(clazz)) {
            return (T) Boolean.valueOf(valStr);
        } else if (Long.class.equals(clazz) || long.class.equals(clazz)) {
            return (T) Long.valueOf(valStr);
        } else if (Integer.class.equals(clazz) || int.class.equals(clazz)) {
            return (T) Integer.valueOf(valStr);
        } else if (Short.class.equals(clazz) || short.class.equals(clazz)) {
            return (T) Short.valueOf(valStr);
        } else if (Float.class.equals(clazz) || float.class.equals(clazz)) {
            return (T) Float.valueOf(valStr);
        } else if (Double.class.equals(clazz) || double.class.equals(clazz)) {
            return (T) Double.valueOf(valStr);
        } else if (Byte.class.equals(clazz) || byte.class.equals(clazz)) {
            return (T) Byte.valueOf(valStr);
        } else if (BigDecimal.class.equals(clazz)) {
            return (T) new BigDecimal(valStr);
        } else {
            throw new IllegalArgumentException("key '" + bindKey + "' is not a illegal type");
        }
    }

    private static void bindProperties(Map<String, Object> map, Type type, Object model, String bindKey) throws NoSuchFieldException, InstantiationException, IllegalAccessException {
        for (var key : map.keySet()) {
            var fieldName = parseFieldName(key);
            var field = (Field) null;
            try {
                field = getField(type, fieldName);
            } catch (NoSuchFieldException e) {
                continue;
            }
            field.setAccessible(true);
            var fieldType = field.getGenericType();
            field.set(model, bindModel(map.get(key), fieldType, bindKey + "." + key));
        }
    }

    private static Class getClassFromType(Type type) {
        var clazz = (Class) null;
        if (type instanceof ParameterizedType) {
            clazz = (Class) ((ParameterizedType) type).getRawType();
        } else {
            clazz = (Class) type;
        }
        return clazz;
    }

    private static String parseFieldName(String key) {
        var splitStrArr = key.split("[_-]");
        for (var i = 0; i < splitStrArr.length; i++) {
            splitStrArr[i] = splitStrArr[i].substring(0, 1).toUpperCase() + splitStrArr[i].substring(1);
        }
        var result = String.join("", splitStrArr);
        result = result.substring(0, 1).toLowerCase() + result.substring(1);
        return result;
    }

    private static <T> T newInstance(Type type) throws InstantiationException, IllegalAccessException {
        var clazz = getClassFromType(type);
        if (clazz.isInterface()) {
            return (T) newInterfaceImpl(clazz);
        } else {
            return (T) clazz.newInstance();
        }
    }

    private static <T> T newInterfaceImpl(Class<T> clazz) {
        if (clazz == List.class) {
            return (T) new ArrayList();
        } else if (clazz == Map.class) {
            return (T) new HashMap();
        } else {
            throw new IllegalArgumentException("not support interface " + clazz);
        }
    }

    private static <T> Field getField(Type type, String fieldName) throws NoSuchFieldException {
        var clazz = getClassFromType(type);
        return clazz.getDeclaredField(fieldName);
    }


    public static <T> T read(String content) {
        return getYaml().load(content);
    }
}
