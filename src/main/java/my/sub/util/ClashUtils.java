package my.sub.util;

import com.xiaoleilu.hutool.io.FileUtil;
import lombok.var;
import my.sub.model.ClashConfig;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClashUtils {

    /**
     * 从文件清单中或者合并后的配置
     *
     * @param configFileList
     * @param workDir
     * @param charset
     * @return
     */
    public static Map<String, Object> getMergeConfigFromFileList(List<String> configFileList, String workDir, String charset) {
        var result = new HashMap<String, Object>();
        for (var path : configFileList) {
            var content = FileUtil.readString(new File(workDir, path), charset);
            var config = (Map<String, Object>) YamlUtil.read(content);
            if (config != null)
                mergeYamlConfig(result, config);
        }
        return result;
    }

    /**
     * 合并yaml配置
     *
     * @param mainConfig
     * @param attachConfig
     */
    public static void mergeYamlConfig(Map<String, Object> mainConfig, Map<String, Object> attachConfig) {
        for (var key : attachConfig.keySet()) {
            var attachValue = attachConfig.get(key);
            var mainValue = mainConfig.get(key);
            if (mainValue == null) {
                mainConfig.put(key, attachValue);
            } else {
                if (mainValue instanceof Map) {
                    mergeYamlConfig((Map<String, Object>) mainValue, (Map<String, Object>) attachValue);
                } else if (mainValue instanceof List) {
                    ((List) mainValue).addAll((List) attachValue);
                } else {
                    mainConfig.put(key, attachValue);
                }
            }
        }
    }

    public static String getSubInfoString(Map<String, Object> config) {
//        var clashConfig = YamlUtil.deserialize(YamlUtil.serialize(config), ClashConfig.class);
//        return YamlUtil.serialize(clashConfig);
        return YamlUtil.serialize(config);
    }
}
