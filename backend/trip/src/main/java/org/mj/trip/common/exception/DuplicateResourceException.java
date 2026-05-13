package org.mj.trip.common.exception;

public class DuplicateResourceException extends RuntimeException {

    private final String field;
    private final String reason;

    public DuplicateResourceException(String message) {
        super(message);
        this.field = null;
        this.reason = message;
    }

    public DuplicateResourceException(String field, String reason) {
        super(reason);
        this.field = field;
        this.reason = reason;
    }

    public String getField() {
        return field;
    }

    public String getReason() {
        return reason;
    }
}
