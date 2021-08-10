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
        log.info(message == null ? "PropertyGroup " + name + " created." : message.toString());
    }

    public String getShopwareUUID() {
        String id = UUID.randomUUID().toString();
        return id.replace("-", "");
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
                .stock(10)
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

        Mono<Object> mediaUploadResult = shopwareHttpClient.uploadMediaResourceUrl(mediaId, mediaUrlUpload, "png");
        message = mediaUploadResult.block();
        log.info(message == null ? "Media OK. ID: " + mediaId : message.toString());

        return productId;
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
                .stock(10)
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
        log.info(resultMessage == null ? "Variant OK. ID: " + variantId : resultMessage.toString());


        if (previewOpt.isPresent()) {
            File preview = previewOpt.get();

            MediaUrlUpload variantMediaUrlUpload = MediaUrlUpload.builder()
                    .url(preview.getPreview_url())
                    .build();

            Mono<Object> variantMediaUploadResult = shopwareHttpClient.uploadMediaResourceUrl(variantMediaId, variantMediaUrlUpload, "png");
            Object variantMessage = variantMediaUploadResult.block();
            log.info(variantMessage == null ? "VariantMedia OK. ID: " + variantMediaId : variantMessage.toString());

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
        printfulCatalogVariants.forEach((id, catalogVariant) -> printfulSizes.add(catalogVariant.getSize()));

        Map<String, String> printfulColors = new HashMap<>();
        printfulCatalogVariants.forEach((id, catalogVariant) -> printfulColors.put(catalogVariant.getColor(), catalogVariant.getColor_code()));

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
        result.put(shopwareSyncProperties.getSizeOptionGroupName(), sizePropertyGroupOptions);
        result.put(shopwareSyncProperties.getColorOptionGroupName(), colorPropertyGroupOptions);

        return result;
    }

    private void createNewPropertyOption(String propertyGroupId, CreatePropertyGroupOption createPropertyGroupOption) {

        Mono<Object> response = shopwareHttpClient.createPropertyGroupOption(propertyGroupId, createPropertyGroupOption);
        Object message = response.block();
        log.info(message == null ? "PropertyGroupOption " + createPropertyGroupOption.getName() + " created." : message.toString());
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

}
