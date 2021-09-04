package cc.raupach.sync.printful.dto;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SyncVariant {

    private BigInteger id;
    private String external_id;
    private BigInteger sync_product_id;
    private String name;
    private Boolean synced;
    private BigInteger variant_id;
    private Double retail_price;
    private String currency;
    private BigInteger warehouse_product_variant_id;
    private String sku;
    private Boolean is_ignored;
    private ProductVariant product;
    private List<File> files = new ArrayList<>();

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
