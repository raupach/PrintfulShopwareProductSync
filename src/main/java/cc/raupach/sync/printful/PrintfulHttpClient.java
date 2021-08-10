package cc.raupach.sync.printful;

import cc.raupach.sync.config.PrintfulSyncProperties;
import cc.raupach.sync.printful.dto.CatalogVariantDetail;
import cc.raupach.sync.printful.dto.ProductRequest;
import cc.raupach.sync.printful.dto.ProductDetailRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigInteger;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@Slf4j
public class PrintfulHttpClient {

    public static final String STORE_PRODUCTS = "store/products";
    public static final String PRODUCTS_VARIANT = "products/variant";

    @Autowired
    private PrintfulSyncProperties printfulSyncProperties;

    @Autowired
    @Qualifier("printful")
    private WebClient printfulWebClient;

    public ProductRequest getProducts() {

        return printfulWebClient.get()
                .uri(printfulSyncProperties.getUrl() + STORE_PRODUCTS)
                .header("Authorization", "Basic " + Base64Utils.encodeToString(printfulSyncProperties.getApiKey().getBytes(UTF_8)))
                .retrieve()
                .bodyToMono(ProductRequest.class)
                .block();
    }


    public ProductDetailRequest getProductDetail(BigInteger id) {

        return printfulWebClient.get()
                .uri(printfulSyncProperties.getUrl() + STORE_PRODUCTS + "/" +id)
                .header("Authorization", "Basic " + Base64Utils.encodeToString(printfulSyncProperties.getApiKey().getBytes(UTF_8)))
                .retrieve()
                .bodyToMono(ProductDetailRequest.class)
                .block();
    }

    public Mono<CatalogVariantDetail> getCatalogVariant(BigInteger variantId) {

        log.debug("getCatalogVariant({}})", variantId);

        return printfulWebClient.get()
                .uri(printfulSyncProperties.getUrl() + PRODUCTS_VARIANT + "/" + variantId)
                .header("Authorization", "Basic " + Base64Utils.encodeToString(printfulSyncProperties.getApiKey().getBytes(UTF_8)))
                .retrieve()
                .bodyToMono(CatalogVariantDetail.class);

    }

    public Flux<CatalogVariantDetail> getCatalogVariant(List<BigInteger> variantIds) {

        return Flux.fromIterable(variantIds)
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .flatMap(this::getCatalogVariant)
                .ordered((u1, u2) -> u2.getResult().getVariant().getId().compareTo(u1.getResult().getVariant().getId()));
    }
}
