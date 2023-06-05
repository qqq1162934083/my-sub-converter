package my.sub.controller;

import com.xiaoleilu.hutool.http.HttpRequest;
import com.xiaoleilu.hutool.io.FileUtil;
import lombok.var;
import my.sub.config.SubConfig;
import my.sub.model.BizException;
import my.sub.model.ClashConfig;
import my.sub.model.RequestSubServerInfoResult;
import my.sub.model.RequestSubServerInput;
import my.sub.model.sub.ConfigInfo;
import my.sub.model.sub.SubInfo;
import my.sub.model.sub.UserConfig;
import my.sub.util.ClashUtils;
import my.sub.util.YamlUtil;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * 获取订阅内容
     * 格式为 /sub/home 其中 home 为配置名称，其他为固定字符
     *
     * @param request
     * @return
     * @throws IOException
     * @throws NoSuchFieldException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @RequestMapping("/**")
    public String sub(HttpServletRequest request) throws IOException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        try {
            //根据请求地址获取配置名称，以确定映射到哪个配置
            final String splitter = "/sub/";
            var url = request.getRequestURL();
            var configName = url.substring(url.indexOf(splitter) + splitter.length());
            var userConfig = subConfig.getUserConfig();
            //获取配置名对应的配置然后执行获取配置逻辑
            var configInfo = userConfig.getConfigInfoList().stream().filter(x -> x.getName().equals(configName)).findFirst().orElseThrow(() -> new RuntimeException("未找到配置"));
            return getConfig(userConfig, configInfo);
        } catch (BizException exp) {
            return "catch exp: " + exp.getMessage();
        }
    }

    /**
     * 获取配置
     *
     * @param userConfig
     * @param configInfo
     * @return
     * @throws IOException
     * @throws NoSuchFieldException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private String getConfig(UserConfig userConfig, ConfigInfo configInfo) throws IOException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        //准备参数
        var cliType = configInfo.getCliType();
        var workDir = subConfig.getWorkDir();
        var charset = userConfig.getCharset();
        var subConverterUrl = userConfig.getSubConverterUrl();
        var subInfoNameList = configInfo.getIncludeSubList();
        if (subInfoNameList == null) subInfoNameList = new ArrayList<>();
        List<String> finalSubInfoNameList = subInfoNameList;
        var subInfoList = userConfig.getSubInfoList().stream().filter(x -> finalSubInfoNameList.contains(x.getName())).collect(Collectors.toList());
        var configFileList = configInfo.getIncludeConfigList();

        //获取代理列表
        var proxyList = new ArrayList<>();
        for (var subInfo : subInfoList) {
            proxyList.addAll(getProxyList(subInfo, workDir, subConverterUrl, cliType, charset));
        }

        //获取所有配置进行合并
        var config = mergeConfigList(configFileList, workDir, cliType, charset);

        //合并代理列表
        mergeProxyList(config, proxyList, cliType);

        return getSubInfoString(config, cliType);
    }

    private String getSubInfoString(Map<String, Object> config, String cliType) {
        switch (cliType) {
            case "clash":
                return ClashUtils.getSubInfoString(config);
            default:
                throw new RuntimeException("未知的cli类型");
        }
    }

    private void mergeProxyList(Map<String, Object> config, ArrayList<Object> proxyList, String cliType) {
        switch (cliType) {
            case "clash":
                var attachConfig = new HashMap<String, Object>();
                attachConfig.put("proxies", proxyList);

                ClashUtils.mergeYamlConfig(config, attachConfig);

                var proxies = YamlUtil.getList(config, "proxies");
                if (proxies != null) {
                    var nameList = proxies.stream().map(x -> YamlUtil.getObject(x, "name")).collect(Collectors.toList());
                    var mainSelectProxyGroup = new HashMap<String, Object>();
                    mainSelectProxyGroup.put("name", "MAIN_SELECT");
                    mainSelectProxyGroup.put("type", "select");
                    mainSelectProxyGroup.put("proxies", nameList);
                    var proxyGroups = new ArrayList<>();
                    proxyGroups.add(mainSelectProxyGroup);
                    attachConfig = new HashMap<>();
                    attachConfig.put("proxy-groups", proxyGroups);
                    ClashUtils.mergeYamlConfig(config, attachConfig);
                }

                break;
            default:
                throw new RuntimeException("未知的cli类型");
        }
    }

    private Map<String, Object> mergeConfigList(List<String> configFileList, String workDir, String cliType, String charset) {
        switch (cliType) {
            case "clash":
                return ClashUtils.getMergeConfigFromFileList(configFileList, workDir, charset);
            default:
                throw new RuntimeException("未知的cli类型");
        }
    }

    private List getProxyList(SubInfo subInfo, String workDir, String converterUrl, String type, String charset) {
        switch (subInfo.getSubType()) {
            case URL:
                return getProxyListByUrl(subInfo, subInfo.getSubUrl(), converterUrl, type, charset);
            case FILE:
                return getProxyListByFile(subInfo, subInfo.getIncludeFileList(), workDir, converterUrl, type, charset);
            default:
                throw new RuntimeException("未知的sub类型");
        }
    }

    private List getProxyListByFile(SubInfo subInfo, List<String> filePathList, String workDir, String converterUrl, String cliType, String charset) {
        var proxyList = new ArrayList<String>();
        var fileList = filePathList.stream().map(x -> new File(workDir, x))
                .collect(Collectors.toList());
        for (var file : fileList) {
            var fileContent = FileUtil.readString(file, charset);
            var subItemList = Arrays.stream(fileContent.split("\n")).map(String::trim).collect(Collectors.toList());
            for (var subItem : subItemList) {
                proxyList.addAll(getProxyListByUrl(subInfo, subItem, converterUrl, cliType, charset));
            }
        }
        return proxyList;
    }

    private List getProxyListByUrl(SubInfo subInfo, String url, String converterUrl, String cliType, String charset) {
        url = URLEncoder.encode(url);
        url = converterUrl + "/sub?target=" + cliType + "&url=" + url;
        var request = HttpRequest.get(url).charset(charset);
        var body = (String) null;
        try {
            body = request.execute().body();
        } catch (Exception exp) {
            throw new BizException(String.format("依赖的服务项没有启动", subInfo.getSubUrl(), converterUrl, exp.getMessage()));
        }
        return getProxyListByCliType(subInfo, body, cliType);
    }

    private List getProxyListByCliType(SubInfo subInfo, String body, String cliType) {
        switch (cliType) {
            case "clash":
                var doc = (Map<String, Object>) null;
                try {
                    doc = YamlUtil.read(body);
                } catch (Exception exp) {
                    throw new BizException(String.format("订阅内容格式错误,订阅地址:%s,内容:%s", subInfo.getSubUrl(), body));
                }
                doc.keySet().stream().filter(x -> !Objects.equals(x, "proxies")).collect(Collectors.toList()).forEach(doc::remove);
                var clashConfig = YamlUtil.deserialize(YamlUtil.serialize(doc), ClashConfig.class);
                var proxies = clashConfig.getProxies();
                if (proxies != null) {
                    for (var proxyObj :
                            proxies) {
                        var proxy = (Map<String, Object>) proxyObj;
                        var name = YamlUtil.getObject(proxyObj, "name");
                        name = subInfo.getName() + "-" + name;
                        proxy.put("name", name);
                    }
                }
                return clashConfig.getProxies();
            default:
                throw new RuntimeException("未知的cli类型");
        }
    }
}
