package my.sub.config;

import com.xiaoleilu.hutool.io.FileUtil;
import lombok.Data;
import lombok.var;
import my.sub.util.YamlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import java.beans.Transient;
import java.io.File;
import java.util.Map;

@Validated
@Data
@Component
@ConfigurationProperties(prefix = "sub-config")
public class SubConfig implements ApplicationRunner {

    @NotEmpty
    private String configPath;

    private String defaultProxyName;

    private String charset;

    private String content;

    private Map<String,Object> configMap;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        File file = new File(configPath);
        if (!file.exists()) {
            throw new RuntimeException("Config file not found: " + configPath);
        }

        content = FileUtil.readString(file, charset);
        configMap = YamlUtil.read(content);
    }
}
