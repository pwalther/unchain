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
 * Exception thrown when a secret cannot be retrieved from the secret provider.
 */
public class SecretRetrievalException extends RuntimeException {

    public SecretRetrievalException(String message) {
        super(message);
    }

    public SecretRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}
