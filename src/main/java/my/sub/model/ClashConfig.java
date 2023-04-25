package my.sub.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClashConfig {
    private Integer port;

    @JsonProperty("socks-port")
    private Integer socksPort;

    @JsonProperty("redir-port")
    private Integer redirPort;

    @JsonProperty("mixed-port")
    private Integer mixedPort;

    @JsonProperty("allow-lan")
    private Boolean allowLan;

    private String mode;

    @JsonProperty("log-level")
    private String logLevel;

    private Boolean ipv6;

    private List<Object> hosts;

    @JsonProperty("external-controller")
    private String externalController;

    @JsonProperty("clash-for-android")
    private String clashForAndroid;

    private String profile;

    private Object dns;

    private List<Object> proxies;

    @JsonProperty("proxy-groups")
    private List<Object> proxyGroups;

    private List<Object> rules;

    @JsonProperty("rule-providers")
    private List<Object> ruleProviders;
}