package my.sub.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoleilu.hutool.http.HttpRequest;
import com.xiaoleilu.hutool.io.FileUtil;
import com.xiaoleilu.hutool.json.JSONUtil;
import com.xiaoleilu.hutool.map.MapUtil;
import lombok.var;
import my.sub.config.SubConfig;
import my.sub.model.RequestSubServerInfoResult;
import my.sub.model.RequestSubServerInput;
import my.sub.model.SubInfo;
import my.sub.util.YamlUtil;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;

@RestController
@RequestMapping("/sub")
public class IndexController {

    @Resource
    private SubConfig subConfig;

    private RequestSubServerInfoResult requestSubServerInfo(RequestSubServerInput input) {
        //执行请求
        var request = HttpRequest.get(input.getUrl());
        input.getRequestHeaders().forEach(request::header);
        var response = request.execute();
        var body = response.body();
        //获取保留的头
        var nullObject = new Object();
        var deliveryHeaders = input.getDeliveryHeaders().stream().collect(Collectors.toMap(x -> x, x -> {
            var headerValue = response.header(x);
            if (headerValue == null) return nullObject;
            return headerValue;
        }));
        for (var entrySet : deliveryHeaders.entrySet()) {
            if (entrySet.getValue() == nullObject) entrySet.setValue(null);
        }
        return new RequestSubServerInfoResult(body, deliveryHeaders);
    }

    @RequestMapping("/**")
    public Object sub(HttpServletRequest request) throws IOException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        var url = request.getRequestURL();
        var subUrl = url.substring(url.indexOf("/sub/") + 5);
        var subMap = subConfig.getConfigMap();
        var configList = YamlUtil.getMapList(subMap, "config-list");
        var config = configList.stream().filter(x -> x.containsKey("name") && Objects.equals(x.get("name"), subUrl)).findFirst().orElseThrow(() -> new RuntimeException("未找到配置"));
        return getConfig(config);
    }

    private Object getConfig(Map<String, Object> config) {
        System.out.println();
        return null;
    }
}
