package cc.raupach.sync.shopware.dto;


import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Getter
@Setter
public class Relationships {

    private Relationship parent;
    private Relationship children;
    private Relationship deliveryTime;
    private Relationship tax;
    private Relationship manufacturer;
    private Relationship cover;
    private Relationship featureSet;
    private Relationship prices;
    private Relationship media;
    private Relationship configuratorSettings;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
