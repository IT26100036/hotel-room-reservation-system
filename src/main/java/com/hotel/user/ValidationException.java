package com.hotel.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationException extends RuntimeException {

    private final List<String> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = Collections.singletonList(message);
    }

    public ValidationException(List<String> errors) {
        super(errors != null && !errors.isEmpty() ? String.join(", ", errors) : "Validation error");
        this.errors = errors != null ? new ArrayList<>(errors) : Collections.emptyList();
    }

    public List<String> getErrors() {
        return errors;
    }
}
