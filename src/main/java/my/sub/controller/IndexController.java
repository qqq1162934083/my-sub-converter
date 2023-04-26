package my.sub.controller;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.xiaoleilu.hutool.http.HttpRequest;
import com.xiaoleilu.hutool.http.HttpResponse;
import com.xiaoleilu.hutool.io.FileUtil;
import com.xiaoleilu.hutool.util.ReflectUtil;
import lombok.var;
import my.sub.model.EModelConfig;
import my.sub.model.SubConfig;
import my.sub.util.JsonNodeUtil;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    @GetMapping("/")
    public String index(@RequestHeader Map<String, String> headers, HttpServletResponse httpServletResponse) throws IOException, IllegalAccessException {
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
        return handleBody(body);
    }

    /**
     * 处理返回的信息
     *
     * @param body
     * @return
     * @throws IOException
     */
    private String handleBody(String body) throws IOException {

        var jsonMapper = new ObjectMapper(new YAMLFactory());
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

        var resource = resourceLoader.getResource("classpath:attached-config.yml");
        var customConfigString = FileUtil.readString(resource.getFile(), StandardCharsets.UTF_8);
        resource = resourceLoader.getResource("classpath:model-config.yml");
        var modelConfigString = FileUtil.readString(resource.getFile(), StandardCharsets.UTF_8);

        var config = JsonNodeUtil.asMap(jsonMapper.readTree(body), Object.class);
        var customConfig = JsonNodeUtil.asMap(jsonMapper.readTree(customConfigString), Object.class);
        var modelConfigMap = JsonNodeUtil.asMap(jsonMapper.readTree(modelConfigString), Object.class);

        //预处理配置
        preprocessConfig(config);

        //获取标准化的模式配置
        var modelConfig = getNormalizedModelConfig(customConfig, modelConfigMap);

        //处理配置
        handleConfig(config, customConfig, modelConfig);

        //将处理后的内容转字符串
        return jsonMapper.writeValueAsString(config);
    }

    /**
     * 对配置进行预处理
     *
     * @param config
     */
    private void preprocessConfig(Map<String, Object> config) {
        //更名main-select 从rules、proxy-groups中替换
        List<Object> proxyGroupList = JsonNodeUtil.asList(config.get("proxy-groups"), Object.class);
        for (var proxyGroup : proxyGroupList) {
            var map = JsonNodeUtil.asMap(proxyGroup, Object.class);
            if(map.containsKey("name")){
                var name = map.get("name");
                if(Objects.equals(subConfig.getMainSelectName(),name)){
                    map.put("name",subConfig.getRulesName());
                }
            }
        }
        System.out.println();
    }

    /**
     * 处理配置
     *
     * @param config
     * @param customConfig
     * @param modelConfig
     */
    private void handleConfig(Map<String, Object> config, Map<String, Object> customConfig, Map<String, EModelConfig> modelConfig) {

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
                            config.put(field, Stream.concat(oldList.stream(), newList.stream()).collect(Collectors.toList()));
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
