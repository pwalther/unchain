package ch.redmoon.unchain.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Strategy {
    private String id;
    private String name;
    private Map<String, String> parameters;
}
