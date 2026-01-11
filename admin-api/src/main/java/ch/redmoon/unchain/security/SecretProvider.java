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

/**
 * Interface for retrieving secrets used in audit log integrity verification.
 * Implementations can integrate with various secret management solutions
 * (HashiCorp Vault, AWS Secrets Manager, Azure Key Vault, etc.).
 */
public interface SecretProvider {

    /**
     * Retrieves the secret key used for HMAC computation in audit log integrity.
     * The secret should be at least 32 bytes for adequate security.
     *
     * @return the secret key as a byte array
     * @throws SecretRetrievalException if the secret cannot be retrieved
     */
    byte[] getAuditLogSecret() throws SecretRetrievalException;
}
