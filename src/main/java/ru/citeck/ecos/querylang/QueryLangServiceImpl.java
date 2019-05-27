package ru.citeck.ecos.querylang;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class QueryLangServiceImpl implements QueryLangService {

    private Map<LangConvPair, QueryLangConverter> languageConverters = new ConcurrentHashMap<>();

    @Override
    public void register(QueryLangConverter converter, String fromLang, String toLang) {
        languageConverters.put(new LangConvPair(fromLang, toLang), converter);
    }

    @Override
    public Optional<JsonNode> convertLang(JsonNode query, String fromLang, String toLang) {
        if (Objects.equals(fromLang, toLang)) {
            return Optional.ofNullable(query);
        }
        QueryLangConverter converter = languageConverters.get(new LangConvPair(fromLang, toLang));
        return converter != null ? Optional.ofNullable(converter.convert(query)) : Optional.empty();
    }

    @Override
    public Optional<QueryWithLang> convertLang(JsonNode query, String language, List<String> required) {

        if (required.contains(language)) {
            return Optional.of(new QueryWithLang(query, language));
        }

        for (String prefLanguage : required) {

            LangConvPair langConvKey = new LangConvPair(language, prefLanguage);
            QueryLangConverter langConv = languageConverters.get(langConvKey);
            JsonNode convertedQuery = langConv != null ? langConv.convert(query) : null;

            if (convertedQuery != null) {
                return Optional.of(new QueryWithLang(convertedQuery, prefLanguage));
            }
        }

        return Optional.empty();
    }

    private static class LangConvPair {

        private final String from;
        private final String to;

        LangConvPair(String from, String to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LangConvPair langPair = (LangConvPair) o;
            return Objects.equals(from, langPair.from)
                && Objects.equals(to, langPair.to);
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }
    }
}
