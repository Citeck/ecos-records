package ru.citeck.ecos.records3.record.operation.query.lang;

import java.util.List;
import java.util.Optional;

public interface QueryLangService {

    /**
     * Convert query.
     *
     * @return a query in the toLang language or null if the conversion is not possible
     */
    Optional<Object> convertLang(Object query, String fromLang, String toLang);

    Optional<QueryWithLang> convertLang(Object query, String fromLang, List<String> toLang);

    void register(QueryLangConverter<?, ?> converter, String fromLang, String toLang);
}


