package cc.raupach.sync.printful.dto;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigInteger;
import java.util.Date;

@Getter
@Setter
public class File {

    private BigInteger id;
    private String type;
    private String hash;
    private String url;
    private String filename;
    private String mime_type;
    private Integer size;
    private Integer width;
    private Integer height;
    private Integer dpi;
    private String status;
    private Date created;
    private String thumbnail_url;
    private String preview_url;
    private Boolean visible;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
