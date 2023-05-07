package my.sub.controller;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.xiaoleilu.hutool.http.HttpRequest;
import com.xiaoleilu.hutool.http.HttpResponse;
import com.xiaoleilu.hutool.io.FileUtil;
import com.xiaoleilu.hutool.util.ReflectUtil;
import lombok.var;
import my.sub.model.ClashConfig;
import my.sub.model.EModelConfig;
import my.sub.model.SubConfig;
import my.sub.util.JsonNodeUtil;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.naming.NameNotFoundException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RestController
public class IndexController {
    @Resource
    private SubConfig subConfig;

    @Resource
    private ResourceLoader resourceLoader;

    private final String defaultProxyName = "MY_PROXY";

    @GetMapping("/sub")
    public String index(@RequestHeader Map<String, String> headers, HttpServletResponse httpServletResponse) throws IOException, IllegalAccessException, NameNotFoundException {
        var now = new Date();
        var dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSSSSS");
        var nowTimeString = dateFormat.format(now);
        var uuid = UUID.randomUUID();
        System.out.println(nowTimeString + " : [" + uuid + "] - request a subscription information response");
        HttpResponse resp = HttpRequest.get(subConfig.getUrl())
                .header("User-Agent", subConfig.getType())
                .execute();
        var body = resp.body();
        //设置响应头
        for (String headerName : resp.headers().keySet()) {
            if (headerName != null && subConfig.getHeaderNames().contains(headerName)) {
                httpServletResponse.setHeader(headerName, resp.header(headerName));
            }
        }
        var result = handleBody(body);
        System.out.println(nowTimeString + " : [" + uuid + "] - response successfully");
        return result;
    }

    /**
     * 处理返回的信息
     *
     * @param body
     * @return
     * @throws IOException
     */
    private String handleBody(String body) throws IOException, NameNotFoundException {

        body = preprocessBodyString(body);

        var jsonMapper = new ObjectMapper(new YAMLFactory());
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

        var resource = resourceLoader.getResource("classpath:attached-config.yml");
        var customConfigString = FileUtil.readString(resource.getFile(), StandardCharsets.UTF_8);
        resource = resourceLoader.getResource("classpath:model-config.yml");
        var modelConfigString = FileUtil.readString(resource.getFile(), StandardCharsets.UTF_8);

        var config = JsonNodeUtil.asMap(jsonMapper.readTree(body), Object.class);
        var customConfig = JsonNodeUtil.asMap(jsonMapper.readTree(customConfigString), Object.class);
        var modelConfigMap = JsonNodeUtil.asMap(jsonMapper.readTree(modelConfigString), Object.class);

        //获取标准化的模式配置
        var modelConfig = getNormalizedModelConfig(customConfig, modelConfigMap);

        //处理配置
        handleConfig(config, customConfig, modelConfig);

        //将处理后的内容转字符串
        var result = jsonMapper.writeValueAsString(config);
        var clashConfig = jsonMapper.readValue(result, ClashConfig.class);
        result = jsonMapper.writeValueAsString(clashConfig);

        return result;
    }


    /**
     * 对订阅地址响应体进行预处理
     *
     * @return
     */
    private String preprocessBodyString(String body) {
        return body.replace(subConfig.getSelectNodeProxyGroupName(), defaultProxyName);
    }

    /**
     * 根据位置找到结点
     *
     * @param node
     * @param location
     * @return
     */
    private Object findNodeByLocation(Object node, Queue<String> location) throws NameNotFoundException {
        Object currNode = node;
        if (location == null) return currNode;

        while (!location.isEmpty()) {
            var name = location.poll();
            if (!JsonNodeUtil.isObjectNode(node))
                throw new NameNotFoundException("找不到结点");
            var map = JsonNodeUtil.asMap(node, Object.class);
            currNode = map.get(name);
        }
        return currNode;
    }

    /**
     * 处理配置
     *
     * @param config
     * @param customConfig
     * @param modelConfig
     */
    private void handleConfig(Map<String, Object> config, Map<String, Object> customConfig, Map<String, EModelConfig> modelConfig) throws NameNotFoundException {

        //改节点选择为 MAIN_SELECT
        var proxyGroups = JsonNodeUtil.asList(config.get("proxy-groups"), Object.class)
                .stream().map(x -> JsonNodeUtil.asMap(x, Object.class)).collect(Collectors.toList());
        var proxyGroup = proxyGroups.stream()
                .filter(x -> Objects.equals(x.get("name").toString(), defaultProxyName) || Objects.equals(x.get("name").toString(), "\"" + defaultProxyName + "\"")).findFirst().orElse(null);
        var proxies = JsonNodeUtil.asList(proxyGroup.get("proxies"), Object.class);
        proxies.add("aws");
        if (proxyGroup == null)
            throw new NameNotFoundException("cant find proxy-group named '" + defaultProxyName + "'");
        proxyGroup.put("name", "MAIN_SELECT");
        proxyGroup.put("proxies", proxies);
        config.put("proxy-groups", proxyGroups);

        //按照模式配置进行处理
        for (var field : modelConfig.keySet()) {
            var model = modelConfig.get(field);

            //不存在该字段则创建
            if (!config.containsKey(field)) {
                config.put(field, null);
            }

            //移除
            if (model == EModelConfig.REMOVE) {
                config.remove(field);
            } else {
                var hasValue = customConfig.containsKey(field);
                var newValue = hasValue ? customConfig.get(field) : null;

                if (hasValue) {
                    if (model == EModelConfig.REPLACE) {
                        //替换或者创建
                        config.put(field, newValue);
                    } else if (model == EModelConfig.LIST_POSTADD || model == EModelConfig.LIST_PREADD) {
                        //附加清单项
                        var oldListIterable = (Iterable<Object>) config.get(field);
                        var oldList = oldListIterable == null ? new ArrayList<>() : StreamSupport.stream(oldListIterable.spliterator(), false).collect(Collectors.toList());
                        var newListIterable = (Iterable<Object>) newValue;
                        var newList = newListIterable == null ? new ArrayList<>() : StreamSupport.stream(newListIterable.spliterator(), false).collect(Collectors.toList());
                        if (model == EModelConfig.LIST_POSTADD) {
                            List<Object> result = Stream.concat(oldList.stream(), newList.stream()).collect(Collectors.toList());
                            config.put(field, result);
                        } else if (model == EModelConfig.LIST_PREADD) {
                            config.put(field, Stream.concat(newList.stream(), oldList.stream()).collect(Collectors.toList()));
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取规范化的模式配置
     *
     * @param customConfigTree
     * @param modelConfigMap
     */
    private Map<String, EModelConfig> getNormalizedModelConfig(Map<String, Object> customConfigTree, Map<String, Object> modelConfigMap) {
        var modelConfig = new HashMap<String, EModelConfig>();
        //获取公共键
        modelConfigMap.forEach((k, v) -> modelConfig.put(k, EModelConfig.parse(v.toString())));
        customConfigTree.forEach((k, v) -> {
            if (!modelConfig.containsKey(k)) modelConfig.put(k, EModelConfig.parse(null));
        });
        return modelConfig;
    }
}
