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

        printfulProducts.forEach(product -> {

            // Alle Varianten für das Produkt holen
            List<SyncVariant> variants = printfulService.getProductVariants(product.getId());
            Double price = printfulService.findMainProductPrice(variants);

            // Varianten_Eigenschaften in Shopware anlegen
            Map<BigInteger, CatalogVariant> printfulCatalogVariants = printfulService.getCatalogVariants(variants);
            Map<String, List<PropertyGroupOption>> shopwarePropertyOptions = shopwareService.updatePropertyOptions(shopwarePropertyGroups, printfulCatalogVariants);

            List<ShopwareProduct> existingProducts = shopwareService.getProducts();
            Optional<ShopwareProduct> existingProductOpt = findProduct(product.getId(), existingProducts);
            if (existingProductOpt.isEmpty()) {
                // Hauptprodukt anlegen
                String productId = shopwareService.createProduct(product, price, shopwarePropertyOptions);

                // Varianten anlegen
                Map<String, List<PropertyGroupOption>> finalShopwarePropertyOptions = shopwarePropertyOptions;
                variants.forEach(syncVariant -> shopwareService.createVariant(productId, syncVariant, finalShopwarePropertyOptions, printfulCatalogVariants));
            } else {
                ShopwareProduct existingProduct = existingProductOpt.get();
                shopwareService.updateProductConfiguratorSetting(existingProduct.getId(), shopwarePropertyOptions);
                shopwarePropertyOptions = shopwareService.updatePropertyOptions(shopwarePropertyGroups, printfulCatalogVariants);
                Map<String, List<PropertyGroupOption>> finalShopwarePropertyOptions = shopwarePropertyOptions;
                variants.stream()
                        .filter(variant-> findProduct(variant.getId(), existingProducts).isEmpty())
                        .forEach(variant -> shopwareService.createVariant(existingProduct.getId(), variant, finalShopwarePropertyOptions, printfulCatalogVariants));
            }
        });

    }

    private Optional<ShopwareProduct> findProduct(BigInteger printfulProductId, List<ShopwareProduct> existingProducts) {
        return existingProducts.stream().filter(p -> StringUtils.equals(p.getAttributes().getProductNumber(), printfulProductId.toString())).findFirst();
    }

}
