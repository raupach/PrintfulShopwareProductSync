package cc.raupach.sync.printful;


import cc.raupach.sync.config.PrintfulSyncProperties;
import cc.raupach.sync.printful.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Autowired
    private PrintfulSyncProperties printfulSyncProperties;

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

        SyncVariant v1 = variants.stream().findFirst().orElseThrow();
        BigInteger productId = v1.getProduct().getProduct_id();
        List<CatalogVariant> catalogVariants = printfulHttpClient.getCatalogVariantForProduct(productId);

        List<BigInteger> ids = variants.stream().map(v -> v.getProduct().getVariant_id()).collect(Collectors.toList());

        Map<BigInteger, CatalogVariant> printfulCatalogVariants = catalogVariants.stream()
                .filter(v -> ids.contains(v.getId()))
                .map(this::mapPropertyOptions)
                .collect(Collectors.toMap(CatalogVariant::getId, v -> v));

        log.info("Get {} CatalogVariants", printfulCatalogVariants.size());
        return printfulCatalogVariants;
    }

    private CatalogVariant mapPropertyOptions(CatalogVariant variant) {

        // remove ugly '″'
        variant.setSize(StringUtils.replaceChars(variant.getSize(), "″\"", ""));

        if (printfulSyncProperties.getOptionMappings().containsKey(variant.getSize())) {
            variant.setSize(printfulSyncProperties.getOptionMappings().get(variant.getSize()));
        }

        if (printfulSyncProperties.getOptionMappings().containsKey(variant.getColor())) {
            variant.setSize(printfulSyncProperties.getOptionMappings().get(variant.getColor()));
        }

        return variant;
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
