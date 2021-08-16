package cc.raupach.sync.shopware.dto;


import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Getter
@Setter
public class ProductConfiguratorSettingAttribute {

    private String productId;
    private String versionId;
    private String mediaId;
    private String optionId;
    private Integer position;
    private Double price;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
