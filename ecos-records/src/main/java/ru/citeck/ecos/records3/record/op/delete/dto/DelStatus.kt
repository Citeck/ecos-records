package ru.citeck.ecos.records3.record.op.delete.dto

enum class DelStatus {
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
