package ch.redmoon.unchain.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Variant {
    private String name;
    private int weight;
    private VariantPayload payload;
    private String stickiness;
}
