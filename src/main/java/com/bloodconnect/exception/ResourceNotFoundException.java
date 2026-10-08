package com.bloodconnect.exception;

/**
 * Thrown when findById / findByEmail returns nothing.
 *
 * WHY not just return null: null forces every caller to write a null check and
 * makes it easy to forget one, which is how NullPointerExceptions are born.
 * A checked exception makes "not found" an explicit, handled case.
 */
public class ResourceNotFoundException extends BloodAppException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException forId(String resource, Object id) {
        return new ResourceNotFoundException(resource + " not found with id " + id);
    }
}
