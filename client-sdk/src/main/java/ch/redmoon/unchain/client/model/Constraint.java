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
public class Constraint {
    @JsonProperty("contextName")
    private String contextName;

    @JsonProperty("operator")
    private Operator operator;

    @JsonProperty("values")
    @Builder.Default
    private List<String> values = new ArrayList<>();

    @JsonProperty("caseInsensitive")
    private boolean caseInsensitive;

    @JsonProperty("inverted")
    private boolean inverted;

    public enum Operator {
        IN("IN"),
        NOT_IN("NOT_IN"),
        STR_ENDS_WITH("STR_ENDS_WITH"),
        STR_STARTS_WITH("STR_STARTS_WITH"),
        STR_CONTAINS("STR_CONTAINS"),
        NUM_EQ("NUM_EQ"),
        NUM_GT("NUM_GT"),
        NUM_GTE("NUM_GTE"),
        NUM_LT("NUM_LT"),
        NUM_LTE("NUM_LTE"),
        DATE_AFTER("DATE_AFTER"),
        DATE_BEFORE("DATE_BEFORE"),
        SEMVER_EQ("SEMVER_EQ"),
        SEMVER_GT("SEMVER_GT"),
        SEMVER_LT("SEMVER_LT");

        private final String value;

        Operator(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        public static Operator fromValue(String value) {
            for (Operator b : Operator.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }
}
