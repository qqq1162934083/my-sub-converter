package my.sub.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.xiaoleilu.hutool.http.HttpRequest;
import lombok.var;
import my.sub.model.ClashConfig;
import my.sub.model.SubConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Arrays;

@RestController
public class IndexController {
    @Resource
    private SubConfig subConfig;

    @GetMapping("/")
    public String index() throws JsonProcessingException {
        var converterConfig = subConfig.getSubConverter();

        var url = "http://" + converterConfig.getHost() + ":" + converterConfig.getPort() + "/sub";
        var resp = "";
        try {
            resp = HttpRequest.get(url)
                    .form("url", subConfig.getUrl())
                    .form("target", subConfig.getType())
                    .execute()
                    .body();
        } catch (Exception exp) {
            return "a error has occurred when dialing the subConverter : \r\n" + exp.getMessage();
        }
        ClashConfig clashConfig;
        try{
            var mapper = new ObjectMapper(new YAMLFactory());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,true);
            var jsonNode = mapper.readTree(resp);
            clashConfig = mapper.readValue(resp, ClashConfig.class);
        }catch (Exception exp) {
            return "a error has occurred when deserializing the sub content to object mode : \r\n" + exp.getMessage();
        }


        return "ok";
    }
}
