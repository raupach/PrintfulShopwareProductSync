package cc.raupach.sync.shopware;

import cc.raupach.sync.config.ShopwareSyncProperties;
import cc.raupach.sync.printful.dto.CatalogVariant;
import cc.raupach.sync.printful.dto.File;
import cc.raupach.sync.printful.dto.Product;
import cc.raupach.sync.printful.dto.SyncVariant;
import cc.raupach.sync.shopware.dto.*;
import cc.raupach.sync.shopware.dto.Currency;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class ShopwareService {

    private Map<String, Currency> currencies = new HashMap<>();
    private Map<String, Tax> tax = new HashMap<>();

    @Autowired
    private ShopwareHttpClient shopwareHttpClient;

    @Autowired
    private ShopwareSyncProperties shopwareSyncProperties;

    public Map<String, PropertyGroup> checkAndCreatePrintfulPropertyGroups() {
        boolean createNewGroup = false;

        List<PropertyGroup> shopwarePropertyGroups = shopwareHttpClient.getPropertyGroups();

        // COLOR
        Optional<PropertyGroup> colorOpt = findPropertyGroup(shopwarePropertyGroups, shopwareSyncProperties.getColorOptionGroupName());
        if (colorOpt.isEmpty()) {
            createNewPropertyGroup(shopwareSyncProperties.getColorOptionGroupName());
            createNewGroup = true;
        }

        // SIZE
        Optional<PropertyGroup> sizeOpt = findPropertyGroup(shopwarePropertyGroups, shopwareSyncProperties.getSizeOptionGroupName());
        if (sizeOpt.isEmpty()) {
            createNewPropertyGroup(shopwareSyncProperties.getSizeOptionGroupName());
            createNewGroup = true;
        }

        if (createNewGroup) {
            shopwarePropertyGroups = shopwareHttpClient.getPropertyGroups();
        }

        Map<String, PropertyGroup> propertyGroupMap = new HashMap<>();
        colorOpt = findPropertyGroup(shopwarePropertyGroups, shopwareSyncProperties.getColorOptionGroupName());
        propertyGroupMap.put(shopwareSyncProperties.getColorOptionGroupName(), colorOpt.orElseThrow());

        sizeOpt = findPropertyGroup(shopwarePropertyGroups, shopwareSyncProperties.getSizeOptionGroupName());
        propertyGroupMap.put(shopwareSyncProperties.getSizeOptionGroupName(), sizeOpt.orElseThrow());

        return propertyGroupMap;
    }


    private Optional<PropertyGroup> findPropertyGroup(List<PropertyGroup> shopwarePropertyGroups, String name) {
        return shopwarePropertyGroups.stream()
                .filter(g -> StringUtils.equals(name, g.getAttributes().getName()))
                .findFirst();
    }

    private void createNewPropertyGroup(String name) {
        CreatePropertyGroup createColorPropertyGroup = CreatePropertyGroup.builder()
                .name(name)
                .build();

        Mono<Object> response = shopwareHttpClient.createPropertyGroup(createColorPropertyGroup);
        Object message = response.block();
        if (message != null) {
            throw new RuntimeException( message.toString());
        } else {
            log.info("PropertyGroup {} created.", name);
        }
    }

    public String getShopwareUUID() {
        String id = UUID.randomUUID().toString();
        return id.replace("-", "");
    }


    public List<ShopwareProduct> getProducts() {
        return shopwareHttpClient.getProducts();
    }

    public String createProduct(Product product, Double price, Map<String, List<PropertyGroupOption>> shopwarePropertyOptions) {

        String productId = getShopwareUUID();
        String productMediaId = getShopwareUUID();
        String mediaId = getShopwareUUID();


        ProductPrice productPrice = ProductPrice.builder()
                .gross(price)
                .net(calculateNetPrice(price))
                .linked(true)
                .currencyId(getDefaultCurrency())
                .build();

        CreateProduct shopwareProduct = CreateProduct.builder()
                .id(productId)
                .name(product.getName())
                .productNumber(product.getId().toString())
                .stock(shopwareSyncProperties.getInitialStock())
                .parentId(null)
                .taxId(getTaxId())
                .price(Collections.singletonList(productPrice))
                .media(new ArrayList<>())
                .configuratorSettings(new ArrayList<>())
                .coverId(productMediaId)
                .build();

        shopwarePropertyOptions.get(shopwareSyncProperties.getSizeOptionGroupName()).forEach(option -> shopwareProduct.getConfiguratorSettings().add(ConfiguratorSettings.builder().optionId(option.getId()).build()));
        shopwarePropertyOptions.get(shopwareSyncProperties.getColorOptionGroupName()).forEach(option -> shopwareProduct.getConfiguratorSettings().add(ConfiguratorSettings.builder().optionId(option.getId()).build()));

        ProductMedia productMedia = ProductMedia.builder()
                .id(productMediaId)
                .media(Media.builder()
                        .id(mediaId)
                        .build())
                .build();

        shopwareProduct.getMedia().add(productMedia);

        Mono<Object> shopwareResult = shopwareHttpClient.createProduct(shopwareProduct);
        Object message = shopwareResult.block();
        log.info(message == null ? "Product OK. ID: " + productId : message.toString());

        MediaUrlUpload mediaUrlUpload = MediaUrlUpload.builder()
                .url(product.getThumbnail_url())
                .build();

        String extension = getFormatOfUrl(product.getThumbnail_url());

        Mono<Object> mediaUploadResult = shopwareHttpClient.uploadMediaResourceUrl(mediaId, mediaUrlUpload, extension);
        message = mediaUploadResult.block();
        if (message != null) {
            throw new RuntimeException(message.toString());
        } else {
            log.info("Media OK. ID: {}", mediaId);
        }
        return productId;
    }

    private String getFormatOfUrl(String thumbnail_url) {
        if (StringUtils.endsWithIgnoreCase(thumbnail_url, "png")){
            return "png";
        } else if (StringUtils.endsWithIgnoreCase(thumbnail_url, "jpg")){
            return "jpg";
        } else if (StringUtils.endsWithIgnoreCase(thumbnail_url, "jpeg")){
            return "jpg";
        } else {
            return "jpg";
        }
    }


    public void createVariant(String productId, SyncVariant variant, Map<String, List<PropertyGroupOption>> shopwarePropertyOptions, Map<BigInteger, CatalogVariant> printfulCatalogVariants) {

        BigInteger printfulVariantId = variant.getProduct().getVariant_id();
        CatalogVariant printfulCatalogVariant = printfulCatalogVariants.get(printfulVariantId);
        Optional<PropertyGroupOption> shopwareOptionSize = findOption(shopwarePropertyOptions.get(shopwareSyncProperties.getSizeOptionGroupName()), printfulCatalogVariant.getSize());
        Optional<PropertyGroupOption> shopwareOptionColor = findOption(shopwarePropertyOptions.get(shopwareSyncProperties.getColorOptionGroupName()), printfulCatalogVariant.getColor());

        String variantId = getShopwareUUID();
        CreateProduct shopwareVariantProduct = CreateProduct.builder()
                .id(variantId)
                .productNumber(variant.getId().toString())
                .stock(shopwareSyncProperties.getInitialStock())
                .parentId(productId)
                .taxId(getTaxId())
                .price(Collections.singletonList(ProductPrice.builder()
                        .gross(variant.getRetail_price())
                        .net(calculateNetPrice(variant.getRetail_price()))
                        .linked(true)
                        .currencyId(getCurrencyIdFor(variant.getCurrency()))
                        .build()))
                .media(new ArrayList<>())
                .options(new ArrayList<>())
                .build();


        shopwareOptionSize.ifPresent(propertyGroupOption -> shopwareVariantProduct.getOptions().add(ProductOption.builder()
                .id(propertyGroupOption.getId())
                .build()));

        shopwareOptionColor.ifPresent(propertyGroupOption -> shopwareVariantProduct.getOptions().add(ProductOption.builder()
                .id(propertyGroupOption.getId())
                .build()));

        String variantProductMediaId = getShopwareUUID();
        String variantMediaId = getShopwareUUID();

        Optional<File> previewOpt = variant.getFiles().stream().filter(file -> StringUtils.equals(file.getType(), "preview")).findFirst();
        if (previewOpt.isPresent()) {

            ProductMedia variantProductMedia = ProductMedia.builder()
                    .id(variantProductMediaId)
                    .media(Media.builder()
                            .id(variantMediaId)
                            .build())
                    .build();

            shopwareVariantProduct.getMedia().add(variantProductMedia);
            shopwareVariantProduct.setCoverId(variantProductMediaId);
        }

        Mono<Object> resultVariant = shopwareHttpClient.createProduct(shopwareVariantProduct);
        Object resultMessage = resultVariant.block();
        if (resultMessage != null) {
            throw new RuntimeException(resultMessage.toString());
        } else {
            log.info("Variant OK. ID: {}", variantId);
        }

        if (previewOpt.isPresent()) {
            File preview = previewOpt.get();

            MediaUrlUpload variantMediaUrlUpload = MediaUrlUpload.builder()
                    .url(preview.getPreview_url())
                    .build();

            String extension = getFormatOfUrl(preview.getThumbnail_url());

            Mono<Object> variantMediaUploadResult = shopwareHttpClient.uploadMediaResourceUrl(variantMediaId, variantMediaUrlUpload, extension);
            Object variantMessage = variantMediaUploadResult.block();
            if (variantMessage != null) {
                throw new RuntimeException(variantMessage.toString());
            } else {
                log.info("VariantMedia OK. ID: {}", variantMediaId);
            }
        }
    }

    public Optional<PropertyGroupOption> findOption(List<PropertyGroupOption> sizePropertyGroupOptions, String printfulSize) {
        return sizePropertyGroupOptions.stream()
                .filter(g -> StringUtils.equals(printfulSize, g.getAttributes().getName()))
                .findFirst();
    }

    public Map<String, List<PropertyGroupOption>> updatePropertyOptions(Map<String, PropertyGroup> shopwarePropertyGroups, Map<BigInteger, CatalogVariant> printfulCatalogVariants) {
        AtomicBoolean createNewColorOption = new AtomicBoolean(false);
        AtomicBoolean createNewSizeOption = new AtomicBoolean(false);

        PropertyGroup sizePropertyGroup = shopwarePropertyGroups.get(shopwareSyncProperties.getSizeOptionGroupName());
        PropertyGroup colorPropertyGroup = shopwarePropertyGroups.get(shopwareSyncProperties.getColorOptionGroupName());

        List<PropertyGroupOption> sizePropertyGroupOptions = shopwareHttpClient.getPropertyGroupsOptions(sizePropertyGroup.getId());
        List<PropertyGroupOption> colorPropertyGroupOptions = shopwareHttpClient.getPropertyGroupsOptions(colorPropertyGroup.getId());

        Set<String> printfulSizes = new HashSet<>();
        printfulCatalogVariants.forEach((id, catalogVariant) -> {
            if (StringUtils.isNotBlank(catalogVariant.getSize())) {
                printfulSizes.add(catalogVariant.getSize());
            }
        });

        Map<String, String> printfulColors = new HashMap<>();
        printfulCatalogVariants.forEach((id, catalogVariant) -> {
            if (StringUtils.isNotBlank(catalogVariant.getColor())) {
                printfulColors.put(catalogVariant.getColor(), catalogVariant.getColor_code());
            }
        });

        List<PropertyGroupOption> finalSizePropertyGroupOptions = sizePropertyGroupOptions;
        printfulSizes.forEach(printfulSize -> {
            Optional<PropertyGroupOption> shopwareOptionOpt = findOption(finalSizePropertyGroupOptions, printfulSize);
            if (shopwareOptionOpt.isEmpty()) {
                CreatePropertyGroupOption createPropertyGroupOption = CreatePropertyGroupOption.builder()
                        .name(printfulSize)
                        .build();

                createNewPropertyOption(sizePropertyGroup.getId(), createPropertyGroupOption);
                createNewSizeOption.set(true);
            }
        });

        List<PropertyGroupOption> finalColorPropertyGroupOptions = colorPropertyGroupOptions;
        printfulColors.forEach((color, colorCode) -> {
            Optional<PropertyGroupOption> shopwareOptionOpt = findOption(finalColorPropertyGroupOptions, color);
            if (shopwareOptionOpt.isEmpty()) {
                CreatePropertyGroupOption createPropertyGroupOption = CreatePropertyGroupOption.builder()
                        .name(color)
                        .colorHexCode(colorCode)
                        .build();

                createNewPropertyOption(colorPropertyGroup.getId(), createPropertyGroupOption);
                createNewColorOption.set(true);
            }
        });

        if (createNewColorOption.get()) {
            colorPropertyGroupOptions = shopwareHttpClient.getPropertyGroupsOptions(colorPropertyGroup.getId());
        }

        if (createNewSizeOption.get()) {
            sizePropertyGroupOptions = shopwareHttpClient.getPropertyGroupsOptions(sizePropertyGroup.getId());
        }

        Map<String, List<PropertyGroupOption>> result = new HashMap<>();
        result.put(shopwareSyncProperties.getSizeOptionGroupName(), filterOptionsSize( sizePropertyGroupOptions, printfulSizes) );
        result.put(shopwareSyncProperties.getColorOptionGroupName(), filterOptionsColor(colorPropertyGroupOptions, printfulColors));

        return result;
    }

    private List<PropertyGroupOption> filterOptionsColor(List<PropertyGroupOption> colorPropertyGroupOptions, Map<String, String> printfulColors) {
        return colorPropertyGroupOptions.stream().filter(color -> printfulColors.containsKey(color.getAttributes().getName())).collect(Collectors.toList());
    }

    private List<PropertyGroupOption> filterOptionsSize(List<PropertyGroupOption> sizePropertyGroupOptions, Set<String> printfulSizes) {
        return sizePropertyGroupOptions.stream().filter(size -> printfulSizes.contains(size.getAttributes().getName())).collect(Collectors.toList());
    }

    private void createNewPropertyOption(String propertyGroupId, CreatePropertyGroupOption createPropertyGroupOption) {

        Mono<Object> response = shopwareHttpClient.createPropertyGroupOption(propertyGroupId, createPropertyGroupOption);
        Object message = response.block();
        if (message != null) {
            throw new RuntimeException(message.toString());
        } else {
            log.info("PropertyGroupOption {} created.", createPropertyGroupOption.getName());
        }
    }


    public String getCurrencyIdFor(String isoCode) {
        if (currencies.isEmpty()) {
            updateCurrency();
        }

        Currency currency = currencies.get(isoCode);
        if (currency != null) {
            return currency.getId();
        } else {
            return getDefaultCurrency();
        }
    }

    public String getDefaultCurrency() {
        if (currencies.isEmpty()) {
            updateCurrency();
        }

        Optional<Map.Entry<String, Currency>> currencyEntry = currencies.entrySet().stream()
                .filter(c -> c.getValue().getAttributes().getIsSystemDefault())
                .findFirst();

        if (currencyEntry.isPresent()) {
            return currencyEntry.get().getValue().getId();
        } else {
            throw new RuntimeException("No currency.");
        }
    }

    private void updateCurrency() {
        currencies = shopwareHttpClient.getCurrencies().stream().collect(Collectors.toMap(k -> k.getAttributes().getIsoCode(), v -> v));
    }

    private void updateTax() {
        tax = shopwareHttpClient.getTax().stream().collect(Collectors.toMap(k -> k.getAttributes().getName(), v -> v));
    }

    public Tax getTax() {
        if (tax.isEmpty()) {
            updateTax();
        }

        Tax t = tax.get(shopwareSyncProperties.getTaxName());
        if (t != null) {
            return t;
        } else {
            throw new RuntimeException("No Tax.");
        }
    }

    public String getTaxId() {
        return getTax().getId();
    }

    private Double getTaxRate() {
        return getTax().getAttributes().getTaxRate();
    }

    private Double calculateNetPrice(Double retailPrice) {
        return retailPrice / (1 + (getTaxRate() / 100));
    }

    public void updateProductConfiguratorSetting(String productId, Map<String, List<PropertyGroupOption>> shopwarePropertyOptions) {
        List<ProductConfiguratorSetting> pcs = shopwareHttpClient.getProductConfiguratorSettings();
        List<ProductConfiguratorSetting> existingProductConfig = pcs.stream().filter(s -> StringUtils.equals(s.getAttributes().getProductId(), productId)).collect(Collectors.toList());

        List<PropertyGroupOption> colorOptions = shopwarePropertyOptions.get(shopwareSyncProperties.getColorOptionGroupName());
        List<PropertyGroupOption> sizeOptions = shopwarePropertyOptions.get(shopwareSyncProperties.getSizeOptionGroupName());

        // create new
        Stream.concat(colorOptions.stream(), sizeOptions.stream())
                .forEach(option -> {
                    Optional<ProductConfiguratorSetting> resultOpt = existingProductConfig.stream()
                            .filter(proConfig -> StringUtils.equals(proConfig.getAttributes().getOptionId(), option.getId()))
                            .findFirst();

                    if (resultOpt.isEmpty()) {
                        newProductConfiguratorSetting(productId, option);
                    }
                });

        // delete non existing
        existingProductConfig.forEach(proConfig-> {
            Optional<PropertyGroupOption> exists = Stream.concat(colorOptions.stream(), sizeOptions.stream())
                    .filter(option -> StringUtils.equals(option.getId(), proConfig.getAttributes().getOptionId()))
                    .findAny();

            if (exists.isEmpty()) {
                Mono<Object> response = shopwareHttpClient.deleteProductConfiguratorSettings(proConfig.getId());
                Object message = response.block();
                if (message == null) {
                    log.info("ProductConfiguratorSetting delete {}", proConfig.getId());
                } else {
                    throw new RuntimeException(message.toString());
                }
            }
        });

    }

    private void newProductConfiguratorSetting(String productId, PropertyGroupOption option) {
        CreateProductConfiguratorSetting newProductConfiguratorSetting = CreateProductConfiguratorSetting.builder()
                .productId(productId)
                .optionId(option.getId())
                .id(getShopwareUUID())
                .build();

        Mono<Object> response = shopwareHttpClient.createProductConfiguratorSetting(newProductConfiguratorSetting);
        Object message = response.block();
        if (message == null) {
            log.info("ProductConfiguratorSetting " + newProductConfiguratorSetting.getId() + " created." );
        } else {
            throw new RuntimeException(message.toString());
        }
    }

    public void deleteVariants(List<ShopwareProduct> existingShopwareVariants, List<SyncVariant> printfulVariants) {
        existingShopwareVariants.forEach (variant -> {
            Optional<SyncVariant> exists = printfulVariants.stream()
                    .filter(printfulVariant -> StringUtils.equals(printfulVariant.getId().toString(), variant.getAttributes().getProductNumber()))
                    .findAny();

            if (exists.isEmpty()) {
                // delete
                Mono<Object> response = shopwareHttpClient.deleteProduct(variant.getId());
                Object message = response.block();
                if (message == null) {
                    log.info("Product delete: {}", variant.getId() );
                } else {
                    throw new RuntimeException(message.toString());
                }
            }
        });
    }

    public void clearCache() {
        Mono<ErrorContainer> response = shopwareHttpClient.clearCache();
        Object message = response.block();
        if (message == null) {
            log.info("Shopware cache cleared.");
        } else {
            throw new RuntimeException(message.toString());
        }
    }
}
