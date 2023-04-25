package my.sub.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.xiaoleilu.hutool.http.HttpRequest;
import com.xiaoleilu.hutool.http.HttpResponse;
import com.xiaoleilu.hutool.io.FileUtil;
import com.xiaoleilu.hutool.util.ReflectUtil;
import jdk.internal.org.objectweb.asm.TypeReference;
import lombok.var;
import my.sub.model.ClashConfig;
import my.sub.model.ProxyGroup;
import my.sub.model.SubConfig;
import org.apache.catalina.mbeans.NamingResourcesMBean;
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
            if(headerName!=null&&subConfig.getHeaderNames().contains(headerName)){
                httpServletResponse.setHeader(headerName, resp.header(headerName));
                System.out.println(headerName+"=>"+resp.header(headerName));
            }
        }
        return handleBody(body);
    }

    private String handleBody(String body) throws IOException {

        var jsonMapper = new ObjectMapper(new YAMLFactory());
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

        var resource = resourceLoader.getResource("classpath:attached-config.yml");
        var customConfig = FileUtil.readString(resource.getFile(), StandardCharsets.UTF_8);
        resource = resourceLoader.getResource("classpath:model-config.yml");
        var modelConfig = FileUtil.readString(resource.getFile(), StandardCharsets.UTF_8);

        var customConfigTree = (LinkedHashMap)ReflectUtil.getFieldValue(jsonMapper.readTree(customConfig),"_children");
        var configTree = (LinkedHashMap)ReflectUtil.getFieldValue(jsonMapper.readTree(body),"_children");
        var modelConfigTree = (LinkedHashMap)ReflectUtil.getFieldValue(jsonMapper.readTree(modelConfig),"_children");
        for(var key : customConfigTree.keySet()){
            if(configTree.containsKey(key)){
                //替换或者添加
            }else{

            }
            System.out.println();
        }


        System.out.println();
        return body;



//        var converterConfig = subConfig.getSubConverter();
//
//        var url = "http://" + converterConfig.getHost() + ":" + converterConfig.getPort() + "/sub";
//        //var resp = "";
//        try {
//            resp = HttpRequest.get(url)
//                    .form("url", subConfig.getUrl())
//                    .form("target", subConfig.getType())
//                    .execute()
//                    .body();
//        } catch (Exception exp) {
//            return "a error has occurred when dialing the subConverter : \r\n" + exp.getMessage();
//        }
//        resp = resp.replace("\uD83D\uDD30 节点选择", "PROXY");
//        ClashConfig clashConfig;
//        try {
//            var jsonNode = jsonMapper.readTree(resp);
//            clashConfig = jsonMapper.readValue(resp, ClashConfig.class);
//        } catch (Exception exp) {
//            return "a error has occurred when deserializing the sub content to object mode : \r\n" + exp.getMessage();
//        }
//
//        //改节点选择为MAIN_SELECT
//        var proxyGroups = clashConfig.getProxyGroups();
//        if (proxyGroups != null) {
//            var proxyGroup = proxyGroups.stream().filter(x -> Objects.equals(x.getName(), "PROXY")).findFirst().orElse(null);
//            if (proxyGroup != null) proxyGroup.setName("MAIN_SELECT");
//        }
//
//        //将规则中的所有的节点选择全走默认代理
////        var rules = clashConfig.getRules();
////        if (rules == null) rules = new ArrayList<String>();
////        for (var i = 0; i < rules.size(); i++) {
////            var ruleResult = rules.get(i);
////            if(ruleResult.endsWith("选择")){
////                System.out.println();
////            }
////            ruleResult = ruleResult.replace("MAIN_SELECT", "PROXY");
////            rules.set(i, ruleResult);
////        }
//
//        ClashConfig attachedConfig;
//        var resource = resourceLoader.getResource("classpath:attached-config.yml");
//        var attachedConfigString = FileUtil.readString(resource.getFile(), StandardCharsets.UTF_8);
//        attachedConfig = jsonMapper.readValue(attachedConfigString, ClashConfig.class);
//
//
//        //覆写和附加
//        var fields = ClashConfig.class.getDeclaredFields();
//        for (var field : fields) {
//            var attachedValue = ReflectUtil.getFieldValue(attachedConfig, field);
//            if (attachedValue != null) {
//                //如果是集合进行添加，否则覆写
//                if (field.getType() == List.class) {
//                    var originValue = (List) ReflectUtil.getFieldValue(clashConfig, field);
//                    if (originValue == null) originValue = new ArrayList();
//                    ((List) attachedValue).addAll(originValue);
//                }
//                ReflectUtil.setFieldValue(clashConfig, field, attachedValue);
//            }
//        }
//
//        var result = jsonMapper.writeValueAsString(clashConfig);
//        return result;
    }
}
