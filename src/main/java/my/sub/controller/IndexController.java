package my.sub.controller;

import com.xiaoleilu.hutool.http.HttpRequest;
import com.xiaoleilu.hutool.io.FileUtil;
import lombok.var;
import my.sub.config.SubConfig;
import my.sub.model.ClashConfig;
import my.sub.model.RequestSubServerInfoResult;
import my.sub.model.RequestSubServerInput;
import my.sub.model.sub.ConfigInfo;
import my.sub.model.sub.SubInfo;
import my.sub.model.sub.UserConfig;
import my.sub.util.YamlUtil;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.util.*;
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
    public String sub(HttpServletRequest request) throws IOException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        var url = request.getRequestURL();
        var configName = url.substring(url.indexOf("/sub/") + 5);
        var userConfig = subConfig.getUserConfig();
        var configInfo = userConfig.getConfigInfoList().stream().filter(x -> x.getName().equals(configName)).findFirst().orElseThrow(() -> new RuntimeException("未找到配置"));
        return YamlUtil.serialize(getConfig(userConfig, configInfo));
    }

    private Object getConfig(UserConfig userConfig, ConfigInfo configInfo) throws IOException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        var cliType = configInfo.getCliType();
        var workDir = subConfig.getWorkDir();
        var charset = userConfig.getCharset();
        var subConverterUrl = userConfig.getSubConverterUrl();
        var subInfoNameList = configInfo.getIncludeSubList();
        var subInfoList = userConfig.getSubInfoList().stream().filter(x -> subInfoNameList.contains(x.getName())).collect(Collectors.toList());
        var configFileList = configInfo.getIncludeConfigList();

        //获取代理列表
        var proxyList = new ArrayList<>();
        for (var subInfo : subInfoList) {
            proxyList.addAll(getProxyList(subInfo, workDir, subConverterUrl, cliType, charset));
        }
        //获取配置
        var config = mergeConfigList(configFileList, workDir, cliType, charset);


        return proxyList;
    }

    private void mergeConfigList(List<String> configFileList, String workDir, String cliType, String charset) {
        switch (cliType){
            case "clash":
                mergeConfigListForClash(configFileList, workDir, cliType, charset);
            default:
                throw new RuntimeException("未知的cli类型");
        }
    }

    private void mergeConfigListForClash(List<String> configFileList, String workDir, String cliType, String charset) {
        for(var path: configFileList){
            var content = FileUtil.readString(new File(workDir, path), charset);
            YamlUtil.read(content);
        }
    }

    private List getProxyList(SubInfo subInfo, String workDir, String converterUrl, String type, String charset) {
        switch (subInfo.getSubType()) {
            case URL:
                return getProxyListByUrl(subInfo.getSubUrl(), converterUrl, type, charset);
            case FILE:
                return getProxyListByFile(subInfo.getIncludeFileList(), workDir, converterUrl, type, charset);
            default:
                throw new RuntimeException("未知的sub类型");
        }
    }

    private List getProxyListByFile(List<String> filePathList, String workDir, String converterUrl, String cliType, String charset) {
        var proxyList = new ArrayList<String>();
        var fileList = filePathList.stream().map(x -> new File(workDir, x))
                .collect(Collectors.toList());
        for (var file : fileList) {
            var fileContent = FileUtil.readString(file, charset);
            var subItemList = Arrays.stream(fileContent.split("\n")).map(String::trim).collect(Collectors.toList());
            for (var subItem : subItemList) {
                proxyList.addAll(getProxyListByUrl(subItem, converterUrl, cliType, charset));
            }
        }
        return proxyList;
    }

    private List getProxyListByUrl(String url, String converterUrl, String cliType, String charset) {
        url = URLEncoder.encode(url);
        url = converterUrl + "/sub?target=" + cliType + "&url=" + url;
        var request = HttpRequest.get(url).charset(charset);
        var body = request.execute().body();
        return getProxyListByCliType(body, cliType);
    }

    private List getProxyListByCliType(String body, String cliType) {
        switch (cliType) {
            case "clash":
                var doc = (Map<String, Object>) YamlUtil.read(body);
                doc.keySet().stream().filter(x -> !Objects.equals(x, "proxies")).collect(Collectors.toList()).forEach(doc::remove);
                var clashConfig = YamlUtil.deserialize(YamlUtil.serialize(doc), ClashConfig.class);
                return clashConfig.getProxies();
            default:
                throw new RuntimeException("未知的cli类型");
        }
    }
}
