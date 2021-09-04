package cc.raupach.sync.printful.dto;


import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigInteger;
import java.util.List;

@Getter
@Setter
public class CatalogVariant {

    private BigInteger id;
    private BigInteger product_id;
    private String name;
    private String size;
    private String color;
    private String color_code;
    private String color_code2;
    private String image;
    private String price;
    private Boolean in_stock;
//    private List<String> availability_regions;
    private List<CatalogAvailabilityStatus> availability_status;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
