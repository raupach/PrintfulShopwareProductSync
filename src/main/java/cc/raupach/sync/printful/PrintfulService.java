package cc.raupach.sync.printful;


import cc.raupach.sync.printful.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PrintfulService {

    @Autowired
    private PrintfulHttpClient printfulHttpClient;

    public List<Product> getStoreProducts() {
        ProductRequest productRequest = printfulHttpClient.getProducts();
        log.info("Get {} Printful products: {}",productRequest.getResult().size(), productRequest);
        return productRequest.getResult();
    }

    public List<SyncVariant> getProductVariants(BigInteger productId) {
        ProductDetailRequest productDetailRequest = printfulHttpClient.getProductDetail(productId);
        ProductDetail productDetail = productDetailRequest.getResult();
        List<SyncVariant> variants = productDetail.getSync_variants();
        log.info("Get {} variants for productId {}: {}", variants.size(), productId, variants);
        return variants;
    }

    public Map<BigInteger, CatalogVariant> getCatalogVariants(List<SyncVariant> variants) {
        List<BigInteger> ids = variants.stream().map(v -> v.getProduct().getVariant_id()).collect(Collectors.toList());
        Flux<CatalogVariantDetail> variantDetailFlux = printfulHttpClient.getCatalogVariant(ids);
        Map<BigInteger, CatalogVariant> printfulCatalogVariants = variantDetailFlux
                .collectMap(k -> k.getResult().getVariant().getId(), v -> v.getResult().getVariant()).block();

        log.info("Get {} CatalogVariants", printfulCatalogVariants.size());
        return printfulCatalogVariants;
    }

    public Double findMainProductPrice(List<SyncVariant> variants) {
        Optional<SyncVariant> variantOpt = variants.stream().findFirst();
        if (variantOpt.isPresent()) {
            return variantOpt.get().getRetail_price();
        } else {
            return 20d;
        }
    }
}
