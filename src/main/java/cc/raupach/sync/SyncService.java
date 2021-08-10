package cc.raupach.sync;

import cc.raupach.sync.printful.PrintfulService;
import cc.raupach.sync.printful.dto.CatalogVariant;
import cc.raupach.sync.printful.dto.Product;
import cc.raupach.sync.printful.dto.SyncVariant;
import cc.raupach.sync.shopware.ShopwareService;
import cc.raupach.sync.shopware.dto.PropertyGroup;
import cc.raupach.sync.shopware.dto.PropertyGroupOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

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

            // Hauptprodukt anlegen
            String productId = shopwareService.createProduct(product, price, shopwarePropertyOptions);

            // Varianten anlegen
            variants.forEach(syncVariant -> shopwareService.createVariant(productId, syncVariant, shopwarePropertyOptions, printfulCatalogVariants));

        });


    }

}
