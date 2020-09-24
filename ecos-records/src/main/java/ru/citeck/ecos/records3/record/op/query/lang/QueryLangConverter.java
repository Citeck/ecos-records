package ru.citeck.ecos.records3.record.op.query.lang;

public interface QueryLangConverter<I, O> {

    O convert(I query);
}
