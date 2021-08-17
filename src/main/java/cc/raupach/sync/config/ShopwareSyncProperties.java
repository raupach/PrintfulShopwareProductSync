package cc.raupach.sync.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("sync.shopware")
@Setter
@Getter
public class ShopwareSyncProperties {

    private String url;
    private String taxName;
    private String colorOptionGroupName;
    private String sizeOptionGroupName;
    private Integer initialStock;

}
