package ru.citeck.ecos.records3.record.op.delete;

public enum DelStatus {
    /**
     * Deletion was successful
     */
    OK,
    /**
     * Internal error
     */
    ERROR,
    /**
     * Record is protected from deletion
     */
    PROTECTED,
    /**
     * Operation is not supported
     */
    NOT_SUPPORTED
}
