package cc.raupach.sync.shopware.dto;


import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Getter
@Setter
public class ProductConfiguratorSetting {

    private String id;
    private String type;
    private ProductConfiguratorSettingAttribute attributes;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
