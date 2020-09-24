package ru.citeck.ecos.records3.record.op.query.lang;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.ReflectUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class QueryLangServiceImpl implements QueryLangService {

    private Map<LangConvPair, ConverterInfo> languageConverters = new ConcurrentHashMap<>();

    @Override
    public void register(QueryLangConverter<?, ?> converter, String fromLang, String toLang) {

        List<Class<?>> genericArgs = ReflectUtils.getGenericArgs(converter.getClass(), QueryLangConverter.class);

        @SuppressWarnings("unchecked")
        QueryLangConverter<Object, Object> objConverter = (QueryLangConverter<Object, Object>) converter;
        Class<?> inputType = genericArgs.size() > 0 ? genericArgs.get(0) : Object.class;
        ConverterInfo info = new ConverterInfo(inputType, objConverter);

        languageConverters.put(new LangConvPair(fromLang, toLang), info);
    }

    @Override
    public Optional<Object> convertLang(Object query, String fromLang, String toLang) {
        if (Objects.equals(fromLang, toLang)) {
            return Optional.ofNullable(query);
        }
        ConverterInfo converterInfo = languageConverters.get(new LangConvPair(fromLang, toLang));

        if (converterInfo != null) {
            Object convertedQuery = Json.getMapper().convert(query, converterInfo.getInputQueryType());
            return Optional.ofNullable(converterInfo.converter.convert(convertedQuery));
        }
        return Optional.empty();
    }

    @Override
    public Optional<QueryWithLang> convertLang(Object query, String language, List<String> required) {

        if (required.contains(language)) {
            return Optional.of(new QueryWithLang(query, language));
        }

        for (String prefLanguage : required) {

            LangConvPair langConvKey = new LangConvPair(language, prefLanguage);
            ConverterInfo converterInfo = languageConverters.get(langConvKey);

            Object convertedQuery = null;
            if (converterInfo != null) {
                Object queryForConverter = Json.getMapper().convert(query, converterInfo.getInputQueryType());
                convertedQuery = converterInfo.converter.convert(queryForConverter);
            }

            if (convertedQuery != null) {
                return Optional.of(new QueryWithLang(convertedQuery, prefLanguage));
            }
        }

        return Optional.empty();
    }

    @Data
    @AllArgsConstructor
    private static class ConverterInfo {
        private final Class<?> inputQueryType;
        private final QueryLangConverter<Object, Object> converter;
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
