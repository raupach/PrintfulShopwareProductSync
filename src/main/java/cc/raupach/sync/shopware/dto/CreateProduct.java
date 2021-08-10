package cc.raupach.sync.shopware.dto;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

@Getter
@Setter
@Builder
public class CreateProduct {

    private String id;
    private String parentId;
    private String name;
    private String productNumber;
    private Integer stock;
    private String taxId;
    private List<ProductPrice> price;
    private List<ProductMedia> media;
    private String coverId;
    private List<ProductOption>options;
    private List<ConfiguratorSettings> configuratorSettings;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
