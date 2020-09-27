package ru.citeck.ecos.records3.record.operation.query.dto;

public enum QueryConsistency {
    EVENTUAL, TRANSACTIONAL, DEFAULT, TRANSACTIONAL_IF_POSSIBLE, HYBRID
}
