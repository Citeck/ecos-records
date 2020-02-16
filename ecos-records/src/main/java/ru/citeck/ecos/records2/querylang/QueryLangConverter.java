package ru.citeck.ecos.records2.querylang;

public interface QueryLangConverter<I, O> {

    O convert(I query);
}
