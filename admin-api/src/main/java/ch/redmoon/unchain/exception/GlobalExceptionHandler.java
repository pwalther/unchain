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

package ch.redmoon.unchain.exception;

import ch.redmoon.unchain.api.model.Error;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Error> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMessage());

        String message = "A data integrity violation occurred.";

        // Try to provide a more specific message for unique constraint violations
        if (ex.getMessage() != null && (ex.getMessage().contains("Unique index or primary key violation")
                || ex.getMessage().contains("constraint"))) {
            message = "This operation conflicts with existing data (e.g. name already in use).";
        }

        Error error = new Error().message(message);
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<Error> handleBusinessRuleViolation(BusinessRuleViolationException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        Error error = new Error().message(ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Error> handleNotImplemented(UnsupportedOperationException ex) {
        log.info("Not implemented endpoint called: {}", ex.getMessage());
        Error error = new Error().message(ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_IMPLEMENTED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Error> handleGeneralException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        Error error = new Error().message("An unexpected error occurred: " + ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
