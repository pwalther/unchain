package ch.redmoon.unchain.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Feature {
    private String name;
    private String type;
    private List<FeatureEnvironment> environments;
}
