package my.sub.model.sub;

import lombok.Data;

import java.util.List;


/**
 * 用户引入的配置
 * 总配置
 */
@Data
public class UserConfig {
    /**
     * 配置清单
     */
    private List<ConfigInfo> configInfoList;
    /**
     * 配置转换器地址
     */
    private String subConverterUrl;
    /**
     * 订阅清单
     */
    private List<SubInfo> subInfoList;
    /**
     * 处理编码
     */
    private String charset = "UTF-8";
}
