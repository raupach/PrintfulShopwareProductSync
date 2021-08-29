package cc.raupach.sync.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties("sync.printful")
@Setter
@Getter
public class PrintfulSyncProperties {

    private String apiKey;
    private String url;
    private Map<String,String> optionMappings;

}
