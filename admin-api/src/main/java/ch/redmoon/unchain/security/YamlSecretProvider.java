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

package ch.redmoon.unchain.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Default implementation of SecretProvider that reads the secret from
 * application.yaml.
 * This is suitable for development and demo purposes.
 * For production, implement a custom SecretProvider that integrates with your
 * secret management solution.
 */
@Component
@ConditionalOnMissingBean(SecretProvider.class)
@Slf4j
public class YamlSecretProvider implements SecretProvider {

    private static final String DEFAULT_SECRET = "change-this-secret-in-production-minimum-32-chars";
    private static final int MINIMUM_SECRET_LENGTH = 32;

    private final String secret;

    public YamlSecretProvider(
            @Value("${unchain.audit.integrity.secret:" + DEFAULT_SECRET + "}") String secret) {
        this.secret = secret;

        if (secret.equals(DEFAULT_SECRET)) {
            log.warn("Using default audit log secret! This is insecure for production. " +
                    "Set unchain.audit.integrity.secret in your configuration.");
        }

        if (secret.length() < MINIMUM_SECRET_LENGTH) {
            throw new SecretRetrievalException(
                    "Audit log secret must be at least " + MINIMUM_SECRET_LENGTH + " characters long");
        }

        log.info("YamlSecretProvider initialized for audit log integrity");
    }

    @Override
    public byte[] getAuditLogSecret() throws SecretRetrievalException {
        return secret.getBytes(StandardCharsets.UTF_8);
    }
}
