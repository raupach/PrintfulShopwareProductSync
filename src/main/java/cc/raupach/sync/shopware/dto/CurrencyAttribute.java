package cc.raupach.sync.shopware.dto;


import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Getter
@Setter
public class CurrencyAttribute {

    private Double factor;
    private String symbol;
    private String isoCode;
    private String shortName;
    private String name;
    private Integer position;
    private Boolean isSystemDefault;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
