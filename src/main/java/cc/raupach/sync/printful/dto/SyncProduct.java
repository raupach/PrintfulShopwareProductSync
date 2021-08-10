package cc.raupach.sync.printful.dto;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigInteger;

@Getter
@Setter
public class SyncProduct {

    private BigInteger id;
    private String external_id;
    private String name;
    private Integer variants;
    private Integer synced;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
