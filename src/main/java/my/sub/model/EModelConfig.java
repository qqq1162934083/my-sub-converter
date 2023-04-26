package my.sub.model;

import lombok.var;

import java.util.Arrays;
import java.util.Objects;

public enum EModelConfig {
    /**
     * 替换，默认模式
     */
    REPLACE("replace"),
    /**
     * 移除
     */
    REMOVE("remove"),
    /**
     * 对集合进行前置添加
     */
    LIST_PREADD("list-preadd"),
    /**
     * 对集合进行后置添加
     */
    LIST_POSTADD("list-postadd");

    /**
     * 用于判断是否属于该类型
     */
    private String value;

    public String getValue() {
        return value;
    }

    /**
     * 根据值解析模式类别
     *
     * @param value
     * @return
     */
    public static EModelConfig parse(String value) {
        if (value == null) return REPLACE;
        value = value.replace("^\\s*", "");
        value = value.replace("\\s*$", "");
        if ((value.startsWith("'") && value.endsWith("'")) || (value.startsWith("\"") && value.endsWith("\""))) {
            value = value.substring(1, value.length() - 1);
        }

        if (value.length() == 0) return REPLACE;

        String finalValue = value;
        return Arrays.stream(EModelConfig.values()).filter(x -> Objects.equals(x.getValue(), finalValue))
                .findFirst().orElseThrow(() -> new IllegalStateException("Invalid value : \"" + finalValue + "\""));
    }

    EModelConfig(String value) {
        this.value = value;
    }
}
