package cc.raupach.sync.printful.dto;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigInteger;

@Getter
@Setter
public class Product {

    private BigInteger id;
    private String external_id;
    private String name;
    private Integer variants;
    private Integer synced;
    private String thumbnail_url;
    private Boolean is_ignored;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
