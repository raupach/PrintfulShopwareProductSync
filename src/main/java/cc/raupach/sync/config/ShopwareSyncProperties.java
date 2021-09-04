package cc.raupach.sync.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Setter
@Getter
public class ShopwareSyncProperties {

    @Value("${sync.shopware.url}")
    private String url;

    @Value("${sync.shopware.taxName}")
    private String taxName;

    @Value("${sync.shopware.colorOptionGroupName}")
    private String colorOptionGroupName;

    @Value("${sync.shopware.sizeOptionGroupName}")
    private String sizeOptionGroupName;

    @Value("${sync.shopware.initialStock}")
    private Integer initialStock;

}
