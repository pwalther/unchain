/*
   Copyright 2026 Philipp Walther

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package ch.redmoon.unchain.service;

import ch.redmoon.unchain.entity.AuditLogEntity;
import ch.redmoon.unchain.security.SecretProvider;
import ch.redmoon.unchain.security.SecretRetrievalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

/**
 * Service for computing and verifying HMAC signatures on audit log entries.
 * Implements hash chaining to detect deletions and tampering.
 */
@Service
@Slf4j
public class AuditLogIntegrityService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String FIELD_SEPARATOR = "|";

    private final SecretProvider secretProvider;
    private final boolean enabled;

    public AuditLogIntegrityService(
            SecretProvider secretProvider,
            @Value("${unchain.audit.integrity.enabled:false}") boolean enabled) {
        this.secretProvider = secretProvider;
        this.enabled = enabled;

        if (enabled) {
            log.info("Audit log integrity protection is ENABLED");
        } else {
            log.info("Audit log integrity protection is DISABLED");
        }
    }

    /**
     * @return true if integrity checking is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Computes the HMAC-SHA256 signature for an audit log entry.
     *
     * @param entity       the audit log entity
     * @param previousHash the hash of the previous entry in the chain (null for
     *                     first entry)
     * @return the Base64-encoded signature
     */
    public String computeSignature(AuditLogEntity entity, String previousHash) {
        if (!enabled) {
            return null;
        }

        try {
            String canonical = getCanonicalRepresentation(entity, previousHash);
            byte[] secretKey = secretProvider.getAuditLogSecret();

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey, HMAC_ALGORITHM);
            mac.init(keySpec);

            byte[] signature = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature);

        } catch (NoSuchAlgorithmException e) {
            log.error("HMAC algorithm not available: {}", HMAC_ALGORITHM, e);
            throw new RuntimeException("Failed to compute audit log signature", e);
        } catch (InvalidKeyException e) {
            log.error("Invalid secret key for HMAC", e);
            throw new RuntimeException("Failed to compute audit log signature", e);
        } catch (SecretRetrievalException e) {
            log.error("Failed to retrieve audit log secret", e);
            throw new RuntimeException("Failed to compute audit log signature", e);
        }
    }

    /**
     * Verifies the signature of an audit log entry.
     *
     * @param entity the audit log entity to verify
     * @return true if the signature is valid, false otherwise
     */
    public boolean verifySignature(AuditLogEntity entity) {
        if (!enabled || entity.getSignature() == null) {
            return true; // If disabled or no signature, consider it valid
        }

        try {
            String computedSignature = computeSignature(entity, entity.getPreviousHash());
            return entity.getSignature().equals(computedSignature);
        } catch (Exception e) {
            log.error("Error verifying signature for audit log ID {}", entity.getId(), e);
            return false;
        }
    }

    /**
     * Verifies the integrity of a chain of audit log entries.
     * Checks both signatures and hash chain continuity.
     *
     * @param entries list of audit log entries in chronological order
     * @return verification result with details
     */
    public ChainVerificationResult verifyChain(List<AuditLogEntity> entries) {
        if (!enabled || entries.isEmpty()) {
            return new ChainVerificationResult(true, true, -1);
        }

        boolean allSignaturesValid = true;
        boolean chainIntact = true;
        int firstInvalidIndex = -1;

        String expectedPreviousHash = null;

        for (int i = 0; i < entries.size(); i++) {
            AuditLogEntity entry = entries.get(i);

            // Verify signature
            if (!verifySignature(entry)) {
                allSignaturesValid = false;
                if (firstInvalidIndex == -1) {
                    firstInvalidIndex = i;
                }
            }

            // Verify chain continuity
            String actualPreviousHash = entry.getPreviousHash();
            if (!java.util.Objects.equals(expectedPreviousHash, actualPreviousHash)) {
                chainIntact = false;
                if (firstInvalidIndex == -1) {
                    firstInvalidIndex = i;
                }
                log.warn("Chain break detected at index {}: expected previousHash={}, actual={}",
                        i, expectedPreviousHash, actualPreviousHash);
            }

            // Compute hash for next iteration
            expectedPreviousHash = computeHash(entry);
        }

        return new ChainVerificationResult(allSignaturesValid, chainIntact, firstInvalidIndex);
    }

    /**
     * Computes the hash of an audit log entry (used for chain linking).
     *
     * @param entity the audit log entity
     * @return the Base64-encoded hash
     */
    public String computeHash(AuditLogEntity entity) {
        if (!enabled) {
            return null;
        }

        // The hash is simply the signature (HMAC output)
        return entity.getSignature();
    }

    /**
     * Creates a canonical string representation of an audit log entry for signing.
     *
     * @param entity       the audit log entity
     * @param previousHash the hash of the previous entry
     * @return canonical representation
     */
    private String getCanonicalRepresentation(AuditLogEntity entity, String previousHash) {
        return String.join(FIELD_SEPARATOR,
                nullSafe(entity.getId()),
                nullSafe(entity.getEntityType()),
                nullSafe(entity.getEntityId()),
                nullSafe(entity.getAction()),
                nullSafe(entity.getChangedBy()),
                nullSafe(entity.getChangedAt()),
                nullSafe(entity.getProjectId()),
                nullSafe(entity.getEnvironment()),
                nullSafe(entity.getFeatureName()),
                nullSafe(entity.getData()),
                nullSafe(previousHash));
    }

    private String nullSafe(Object value) {
        return value == null ? "" : value.toString();
    }

    /**
     * Result of chain verification.
     */
    public static class ChainVerificationResult {
        private final boolean allSignaturesValid;
        private final boolean chainIntact;
        private final int firstInvalidIndex;

        public ChainVerificationResult(boolean allSignaturesValid, boolean chainIntact, int firstInvalidIndex) {
            this.allSignaturesValid = allSignaturesValid;
            this.chainIntact = chainIntact;
            this.firstInvalidIndex = firstInvalidIndex;
        }

        public boolean isAllSignaturesValid() {
            return allSignaturesValid;
        }

        public boolean isChainIntact() {
            return chainIntact;
        }

        public int getFirstInvalidIndex() {
            return firstInvalidIndex;
        }

        public boolean isValid() {
            return allSignaturesValid && chainIntact;
        }
    }
}
