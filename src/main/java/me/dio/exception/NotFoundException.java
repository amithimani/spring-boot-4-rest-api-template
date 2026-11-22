package me.dio.exception;

public class NotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(Object id) {
        super("Resource ID %s not found.".formatted(id));
    }
}
