package ch.redmoon.unchain.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeatureEnvironment {
    @JsonProperty("name")
    private String name;

    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("strategies")
    @Builder.Default
    private List<Strategy> strategies = new ArrayList<>();
}
