package cc.raupach.sync.shopware;

import cc.raupach.sync.config.ShopwareSyncProperties;
import cc.raupach.sync.shopware.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class ShopwareHttpClient {

    private static final String ACTION_MEDIA = "_action/media/";
    private static final String UPLOAD_EXTENSION = "/upload?extension=";
    private static final String PROPERTY_GROUP = "property-group";
    private static final String PRODUCT = "product";
    private static final String CURRENCY = "currency";
    private static final String TAX = "tax";
    private static final String OPTIONS = "/options";
    private static final String PRODUCT_CONFIGURATOR_SETTING = "product-configurator-setting";

    @Autowired
    @Qualifier("shopware")
    private WebClient shopwareWebClient;

    @Autowired
    private ShopwareSyncProperties shopwareSyncProperties;

    public List<ShopwareProduct> getProducts() {
        ProductResponse currencyResponse = shopwareWebClient.get()
                .uri(shopwareSyncProperties.getUrl() + PRODUCT)
                .retrieve()
                .bodyToMono(ProductResponse.class)
                .block();

        return currencyResponse.getData();
    }

    public Mono<Object> createProduct(CreateProduct product) {

        return shopwareWebClient.post()
                .uri(shopwareSyncProperties.getUrl() + PRODUCT)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(product), CreateProduct.class)
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.NO_CONTENT)) {
                        return Mono.empty();
                    } else if (response.statusCode().is4xxClientError()) {
                        return response.bodyToMono(ErrorContainer.class);
                    } else {
                        return Mono.empty();
                    }
                });
    }


    public Mono<Object> createProductConfiguratorSetting(CreateProductConfiguratorSetting newProductConfiguratorSetting) {
        return shopwareWebClient.post()
                .uri(shopwareSyncProperties.getUrl() + PRODUCT_CONFIGURATOR_SETTING)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(newProductConfiguratorSetting), CreateProductConfiguratorSetting.class)
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.NO_CONTENT)) {
                        return Mono.empty();
                    } else if (response.statusCode().is4xxClientError()) {
                        return response.bodyToMono(ErrorContainer.class);
                    } else {
                        return Mono.empty();
                    }
                });
    }

    public List<ProductConfiguratorSetting> getProductConfiguratorSettings() {
        ProductConfiguratorSettingResponse productConfiguratorSettingResponse = shopwareWebClient.get()
                .uri(shopwareSyncProperties.getUrl() + PRODUCT_CONFIGURATOR_SETTING)
                .retrieve()
                .bodyToMono(ProductConfiguratorSettingResponse.class)
                .block();

        return productConfiguratorSettingResponse.getData();
    }

    public Mono<Object> uploadMediaResourceUrl(String mediaId, MediaUrlUpload mediaUrlUpload, String extension) {

        return shopwareWebClient.post()
                .uri(shopwareSyncProperties.getUrl() + ACTION_MEDIA + mediaId + UPLOAD_EXTENSION + extension)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(mediaUrlUpload), MediaUrlUpload.class)
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.NO_CONTENT)) {
                        return Mono.empty();
                    } else if (response.statusCode().is4xxClientError()) {
                        return response.bodyToMono(ErrorContainer.class);
                    } else {
                        return Mono.empty();
                    }
                });
    }

    public List<PropertyGroup> getPropertyGroups() {

        PropertyGroupResponse propertyGroupResponse = shopwareWebClient.get()
                .uri(shopwareSyncProperties.getUrl() + PROPERTY_GROUP)
                .retrieve()
                .bodyToMono(PropertyGroupResponse.class)
                .block();

        return propertyGroupResponse.getData();
    }

    public Mono<Object> createPropertyGroup(CreatePropertyGroup createColorPropertyGroup) {
        return shopwareWebClient.post()
                .uri(shopwareSyncProperties.getUrl() + PROPERTY_GROUP)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(createColorPropertyGroup), CreatePropertyGroup.class)
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.NO_CONTENT)) {
                        return Mono.empty();
                    } else if (response.statusCode().is4xxClientError()) {
                        return response.bodyToMono(ErrorContainer.class);
                    } else {
                        return Mono.empty();
                    }
                });
    }

    public List<PropertyGroupOption> getPropertyGroupsOptions(String groupId) {

        Mono<PropertyGroupOptionsResponse> propertyGroupOptionsResponseMono = shopwareWebClient.get()
                .uri(shopwareSyncProperties.getUrl() + PROPERTY_GROUP + "/" + groupId + OPTIONS)
                .retrieve()
                .bodyToMono(PropertyGroupOptionsResponse.class);

        PropertyGroupOptionsResponse propertyGroupOptionsResponse = propertyGroupOptionsResponseMono.block();
        return propertyGroupOptionsResponse.getData();
    }

    public Mono<Object> createPropertyGroupOption(String propertyGroupId, CreatePropertyGroupOption createPropertyGroupOption) {
        return shopwareWebClient.post()
                .uri(shopwareSyncProperties.getUrl() + PROPERTY_GROUP + "/" + propertyGroupId + OPTIONS)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(createPropertyGroupOption), CreatePropertyGroupOption.class)
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.NO_CONTENT)) {
                        return Mono.empty();
                    } else if (response.statusCode().is4xxClientError()) {
                        return response.bodyToMono(ErrorContainer.class);
                    } else {
                        return Mono.empty();
                    }
                });
    }

    public List<Currency> getCurrencies() {
        CurrencyResponse currencyResponse = shopwareWebClient.get()
                .uri(shopwareSyncProperties.getUrl() + CURRENCY)
                .retrieve()
                .bodyToMono(CurrencyResponse.class)
                .block();

        return currencyResponse.getData();
    }

    public List<Tax> getTax() {
        TaxResponse currencyResponse = shopwareWebClient.get()
                .uri(shopwareSyncProperties.getUrl() + TAX)
                .retrieve()
                .bodyToMono(TaxResponse.class)
                .block();

        return currencyResponse.getData();
    }

}
