package ch.redmoon.unchain.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VariantPayload {
    private String type;
    private String value;
}
