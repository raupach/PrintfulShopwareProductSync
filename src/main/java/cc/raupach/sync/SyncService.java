package cc.raupach.sync;

import cc.raupach.sync.printful.PrintfulService;
import cc.raupach.sync.printful.dto.CatalogVariant;
import cc.raupach.sync.printful.dto.Product;
import cc.raupach.sync.printful.dto.SyncVariant;
import cc.raupach.sync.shopware.ShopwareService;
import cc.raupach.sync.shopware.dto.PropertyGroup;
import cc.raupach.sync.shopware.dto.PropertyGroupOption;
import cc.raupach.sync.shopware.dto.ShopwareProduct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SyncService implements CommandLineRunner {


    @Autowired
    private ShopwareService shopwareService;

    @Autowired
    private PrintfulService printfulService;


    @Override
    public void run(String... args) throws Exception {

        Map<String, PropertyGroup> shopwarePropertyGroups = shopwareService.checkAndCreatePrintfulPropertyGroups();

        List<Product> printfulProducts = printfulService.getStoreProducts();

        printfulProducts.forEach(printfulProduct -> {

            // Alle Varianten für das Produkt holen
            List<SyncVariant> printfulVariants = printfulService.getProductVariants(printfulProduct.getId());
            Double price = printfulService.findMainProductPrice(printfulVariants);

            // Varianten_Eigenschaften in Shopware anlegen
            Map<BigInteger, CatalogVariant> printfulCatalogVariants = printfulService.getCatalogVariants(printfulVariants);
            Map<String, List<PropertyGroupOption>> shopwarePropertyOptions = shopwareService.updatePropertyOptions(shopwarePropertyGroups, printfulCatalogVariants);

            List<ShopwareProduct> existingProducts = shopwareService.getProducts();
            Optional<ShopwareProduct> existingProductOpt = findProduct(printfulProduct.getId(), existingProducts);
            if (existingProductOpt.isEmpty()) {
                // Hauptprodukt anlegen
                String productId = shopwareService.createProduct(printfulProduct, price, shopwarePropertyOptions);

                // Varianten anlegen
                Map<String, List<PropertyGroupOption>> finalShopwarePropertyOptions = shopwarePropertyOptions;
                printfulVariants.forEach(syncVariant -> shopwareService.createVariant(productId, syncVariant, finalShopwarePropertyOptions, printfulCatalogVariants));
            } else {
                ShopwareProduct existingProduct = existingProductOpt.get();
                List<ShopwareProduct> existingVariants = findVariantsForProduct(existingProduct.getId(), existingProducts);
                shopwareService.deleteVariants(existingVariants, printfulVariants);

                shopwareService.updateProductConfiguratorSetting(existingProduct.getId(), shopwarePropertyOptions);
                shopwarePropertyOptions = shopwareService.updatePropertyOptions(shopwarePropertyGroups, printfulCatalogVariants);
                Map<String, List<PropertyGroupOption>> finalShopwarePropertyOptions = shopwarePropertyOptions;
                printfulVariants.stream()
                        .filter(variant-> findProduct(variant.getId(), existingProducts).isEmpty())
                        .forEach(variant -> shopwareService.createVariant(existingProduct.getId(), variant, finalShopwarePropertyOptions, printfulCatalogVariants));
            }
        });

        shopwareService.clearCache();

    }

    private Optional<ShopwareProduct> findProduct(BigInteger printfulProductId, List<ShopwareProduct> existingProducts) {
        return existingProducts.stream().filter(p -> StringUtils.equals(p.getAttributes().getProductNumber(), printfulProductId.toString())).findFirst();
    }

    private List<ShopwareProduct> findVariantsForProduct(String productId, List<ShopwareProduct> existingProducts) {
        return existingProducts.stream().filter(p -> StringUtils.equals(p.getAttributes().getParentId(), productId)).collect(Collectors.toList());
    }

}
