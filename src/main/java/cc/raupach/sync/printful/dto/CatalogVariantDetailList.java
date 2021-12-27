package cc.raupach.sync.printful.dto;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;


@Getter
@Setter
public class CatalogVariantDetailList extends BaseDto {

    private CatalogVariantDetailListResult result;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
