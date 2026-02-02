package ch.redmoon.unchain.client.strategy;

import ch.redmoon.unchain.client.UnchainContext;
import ch.redmoon.unchain.client.model.Constraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class ConstraintEvaluator {
    private static final Logger log = LoggerFactory.getLogger(ConstraintEvaluator.class);

    public static boolean evaluate(List<Constraint> constraints, UnchainContext context) {
        if (constraints == null || constraints.isEmpty()) {
            return true;
        }

        return constraints.stream().allMatch(constraint -> evaluate(constraint, context));
    }

    public static boolean evaluate(Constraint constraint, UnchainContext context) {
        String contextValue = context.getProperty(constraint.getContextName());

        // Handling special context fields if mapped incorrectly or missing
        if (contextValue == null) {
            String ctxName = constraint.getContextName();
            if ("userId".equalsIgnoreCase(ctxName)) {
                contextValue = context.getUserId();
            } else if ("sessionId".equalsIgnoreCase(ctxName)) {
                contextValue = context.getSessionId();
            } else if ("environment".equalsIgnoreCase(ctxName)) {
                contextValue = context.getEnvironment();
            } else if ("appName".equalsIgnoreCase(ctxName)) {
                // If appName is used, it often comes from config, but here checking property
            } else if ("currentTime".equalsIgnoreCase(ctxName)) {
                contextValue = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            }
        }

        // If field is missing and constraint requires matching against values, it
        // usually fails?
        // But logical operators might behave differently.
        // Typically undefined context -> false unless operator handles it.
        // For IN/NOT_IN, undefined means it's not in list.

        boolean result = evaluateInternal(constraint, contextValue);
        return constraint.isInverted() != result;
    }

    private static boolean evaluateInternal(Constraint constraint, String contextValue) {
        if (contextValue == null && constraint.getOperator() != Constraint.Operator.NOT_IN) {
            // Most operators return false if the value is missing
            return false;
        }

        switch (constraint.getOperator()) {
            case IN:
                return contains(constraint.getValues(), contextValue, constraint.isCaseInsensitive());
            case NOT_IN:
                return !contains(constraint.getValues(), contextValue, constraint.isCaseInsensitive());
            case STR_ENDS_WITH:
                if (contextValue == null)
                    return false;
                for (String val : constraint.getValues()) {
                    if (constraint.isCaseInsensitive()) {
                        if (contextValue.toLowerCase().endsWith(val.toLowerCase()))
                            return true;
                    } else {
                        if (contextValue.endsWith(val))
                            return true;
                    }
                }
                return false;
            case STR_STARTS_WITH:
                if (contextValue == null)
                    return false;
                for (String val : constraint.getValues()) {
                    if (constraint.isCaseInsensitive()) {
                        if (contextValue.toLowerCase().startsWith(val.toLowerCase()))
                            return true;
                    } else {
                        if (contextValue.startsWith(val))
                            return true;
                    }
                }
                return false;
            case STR_CONTAINS:
                if (contextValue == null)
                    return false;
                for (String val : constraint.getValues()) {
                    if (constraint.isCaseInsensitive()) {
                        if (contextValue.toLowerCase().contains(val.toLowerCase()))
                            return true;
                    } else {
                        if (contextValue.contains(val))
                            return true;
                    }
                }
                return false;
            case NUM_EQ:
            case NUM_GT:
            case NUM_GTE:
            case NUM_LT:
            case NUM_LTE:
                return evaluateNumeric(constraint.getOperator(), contextValue, constraint.getValues());
            case DATE_AFTER:
            case DATE_BEFORE:
                return evaluateDate(constraint.getOperator(), contextValue, constraint.getValues());
            case SEMVER_EQ:
            case SEMVER_GT:
            case SEMVER_LT:
                return evaluateSemVer(constraint.getOperator(), contextValue, constraint.getValues());
            default:
                log.warn("Unknown constraint operator: {}", constraint.getOperator());
                return false;
        }
    }

    private static boolean contains(List<String> values, String value, boolean caseInsensitive) {
        if (value == null)
            return false;
        if (values == null)
            return false;
        if (caseInsensitive) {
            String lowerValue = value.toLowerCase();
            return values.stream().anyMatch(v -> v.toLowerCase().equals(lowerValue));
        }
        return values.contains(value);
    }

    private static boolean evaluateNumeric(Constraint.Operator operator, String contextValue, List<String> values) {
        if (contextValue == null || values == null || values.isEmpty())
            return false;
        try {
            double ctxVal = Double.parseDouble(contextValue);
            for (String val : values) {
                double constraintVal = Double.parseDouble(val);
                boolean matches = false;
                switch (operator) {
                    case NUM_EQ:
                        matches = Math.abs(ctxVal - constraintVal) < 0.000001;
                        break;
                    case NUM_GT:
                        matches = ctxVal > constraintVal;
                        break;
                    case NUM_GTE:
                        matches = ctxVal >= constraintVal;
                        break;
                    case NUM_LT:
                        matches = ctxVal < constraintVal;
                        break;
                    case NUM_LTE:
                        matches = ctxVal <= constraintVal;
                        break;
                }
                if (matches)
                    return true;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return false;
    }

    private static boolean evaluateDate(Constraint.Operator operator, String contextValue, List<String> values) {
        if (contextValue == null || values == null || values.isEmpty())
            return false;
        try {
            // Supports ISO-8601
            OffsetDateTime ctxDate = OffsetDateTime.parse(contextValue);
            for (String val : values) {
                OffsetDateTime constraintDate = OffsetDateTime.parse(val);
                boolean matches = false;
                switch (operator) {
                    case DATE_AFTER:
                        matches = ctxDate.isAfter(constraintDate);
                        break;
                    case DATE_BEFORE:
                        matches = ctxDate.isBefore(constraintDate);
                        break;
                }
                if (matches)
                    return true;
            }
        } catch (Exception e) {
            log.debug("Date parse error: {}", e.getMessage());
            return false;
        }
        return false;
    }

    private static boolean evaluateSemVer(Constraint.Operator operator, String contextValue, List<String> values) {
        if (contextValue == null || values == null || values.isEmpty())
            return false;
        // Simple SemVer implementation: MAJOR.MINOR.PATCH[-PRERELEASE][+BUILDMETADATA]
        // Regex for validation could be added, or splitting by dot.
        // Assuming simple major.minor.patch for now to avoid heavy deps.

        for (String val : values) {
            int cmp = compareSemVer(contextValue, val);
            boolean matches = false;
            switch (operator) {
                case SEMVER_EQ:
                    matches = cmp == 0;
                    break;
                case SEMVER_GT:
                    matches = cmp > 0;
                    break;
                case SEMVER_LT:
                    matches = cmp < 0;
                    break;
            }
            if (matches)
                return true;
        }
        return false;
    }

    // Returns >0 if v1 > v2, <0 if v1 < v2, 0 if equal
    private static int compareSemVer(String v1, String v2) {
        // Strip validation for now, assume X.Y.Z
        String[] p1 = v1.split("-")[0].split("\\.");
        String[] p2 = v2.split("-")[0].split("\\.");

        int len = Math.max(p1.length, p2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < p1.length ? parseVer(p1[i]) : 0;
            int n2 = i < p2.length ? parseVer(p2[i]) : 0;
            if (n1 != n2) {
                return n1 - n2;
            }
        }
        // rudimentary prerelease check if main parts equal?
        // For simplicity ignoring prerelease order unless necessary
        return 0;
    }

    private static int parseVer(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

}
