package ch.redmoon.unchain.client.exception;

public class UnchainException extends RuntimeException {
    public UnchainException(String message) {
        super(message);
    }

    public UnchainException(String message, Throwable cause) {
        super(message, cause);
    }
}
