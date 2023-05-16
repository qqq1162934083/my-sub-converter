package my.sub.controller;

import com.fasterxml.jackson.core.type.TypeReference;
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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;

@RestController
public class IndexController {

    @Resource
    private SubConfig subConfig;

    @Resource
    private ResourceLoader resourceLoader;

    private Object getSubInfoConfig() throws IOException {
        var cfgRes = resourceLoader.getResource(subConfig.getConfigPath());
        return new Yaml().load(FileUtil.readString(cfgRes.getFile(), subConfig.getCharset()));
    }

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

    @RequestMapping("/rule")
    public String rule() throws IOException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        var yaml = new Yaml();
        var config = getSubInfoConfig();
        var subList = YamlUtil.getList(config, "sub-list");
        var subInfoList = YamlUtil.parse(subList, new TypeReference<List<SubInfo>>() {
        });
        return JSONUtil.toJsonPrettyStr(subInfoList);
    }

    @RequestMapping("/global")
    public String global() {
        return "123";
    }
}
