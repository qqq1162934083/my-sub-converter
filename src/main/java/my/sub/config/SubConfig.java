package my.sub.config;

import com.xiaoleilu.hutool.io.FileUtil;
import lombok.Data;
import lombok.var;
import my.sub.model.sub.SubType;
import my.sub.model.sub.UserConfig;
import my.sub.util.YamlUtil;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import java.io.File;
import java.util.stream.Collectors;

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

    private String workDir;

    public UserConfig getUserConfig() {
        return YamlUtil.deserialize(content, UserConfig.class);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        File file = new File(configPath);
        if (!file.exists()) {
            throw new RuntimeException("Config file not found: " + configPath);
        }

        content = FileUtil.readString(file, charset);
        var config = getUserConfig();
        var names = config.getConfigInfoList().stream().map(x -> x.getName()).collect(Collectors.toList());
        System.out.println("可用配置：" + String.join(",", names));

        //工作目录
        workDir = file.getParentFile().getAbsolutePath();
    }
}
