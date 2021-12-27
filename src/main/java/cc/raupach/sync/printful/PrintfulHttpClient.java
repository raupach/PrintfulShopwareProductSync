package cc.raupach.sync.printful;

import cc.raupach.sync.config.PrintfulSyncProperties;
import cc.raupach.sync.printful.dto.CatalogVariant;
import cc.raupach.sync.printful.dto.CatalogVariantDetailList;
import cc.raupach.sync.printful.dto.ProductDetailRequest;
import cc.raupach.sync.printful.dto.ProductRequest;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@Slf4j
public class PrintfulHttpClient {

    public static final String STORE_PRODUCTS = "store/products";
    public static final String PRODUCTS = "products/";

    @Autowired
    private PrintfulSyncProperties printfulSyncProperties;

    @Autowired
    @Qualifier("printful")
    private WebClient printfulWebClient;

    final RateLimiter rateLimiter = RateLimiter.create(2.0);
    private String authorization;

    @PostConstruct
    private void setup() {
        authorization =  Base64Utils.encodeToString(printfulSyncProperties.getApiKey().getBytes(UTF_8));
    }


    public ProductRequest getProducts() {
        double sleep = rateLimiter.acquire();
        log.info("Sleep: {}",sleep);
        return printfulWebClient.get()
                .uri(printfulSyncProperties.getUrl() + STORE_PRODUCTS)
                .header("Authorization", "Basic " + authorization)
                .retrieve()
                .bodyToMono(ProductRequest.class)
                .block();
    }


    public ProductDetailRequest getProductDetail(BigInteger id) {
        double sleep = rateLimiter.acquire();
        log.info("Sleep: {}",sleep);
        return printfulWebClient.get()
                .uri(printfulSyncProperties.getUrl() + STORE_PRODUCTS + "/" +id)
                .header("Authorization", "Basic " + authorization)
                .retrieve()
                .bodyToMono(ProductDetailRequest.class)
                .block();
    }


    public List<CatalogVariant> getCatalogVariantForProduct(BigInteger productId) {
        double sleep = rateLimiter.acquire();
        log.info("Sleep: {}",sleep);
        log.debug("getCatalogVariantForProduct({}})", productId);

        CatalogVariantDetailList catalogVariantDetailList = printfulWebClient.get()
                .uri(printfulSyncProperties.getUrl() + PRODUCTS + productId)
                .header("Authorization", "Basic " + authorization)
                .retrieve()
                .bodyToMono(CatalogVariantDetailList.class)
                .block();

        return catalogVariantDetailList.getResult().getVariants();
    }
}
