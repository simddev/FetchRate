package com.fetchrate.core;

/**
 * Thrown when a rate exists for the requested currency but no data is available
 * for the given date. Maps to HTTP 404 in the HTTP adapter layer.
 */
public class RateNotFoundException extends IllegalArgumentException {
    public RateNotFoundException(String message) {
        super(message);
    }
}
