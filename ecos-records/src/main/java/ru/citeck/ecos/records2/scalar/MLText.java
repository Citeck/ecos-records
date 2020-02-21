package ru.citeck.ecos.records2.scalar;

import ecos.com.fasterxml.jackson210.annotation.JsonCreator;
import ecos.com.fasterxml.jackson210.annotation.JsonValue;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@EqualsAndHashCode
public final class MLText implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    /**
     * String values of same text in different locales.
     */
    private Map<Locale, String> localizedValues;

    public MLText() {
        this.localizedValues = new HashMap<>();
    }

    /**
     * Store text with EN locale by default.
     * Note: Jackson use this constructor for mapping simple string to our object.
     *
     * @param content value for default locale
     */
    @JsonCreator
    public MLText(String content) {
        this.localizedValues = new HashMap<>();
        localizedValues.put(DEFAULT_LOCALE, content);
    }

    /**
     * Note: Jackson use this constructor for mapping {key:value} strings to our object.
     *
     * @param localizedValues map with locales inside
     */
    @JsonCreator
    public MLText(Map<Locale, String> localizedValues) {
        this.localizedValues = new HashMap<>(localizedValues);
    }

    @JsonValue
    public Map<Locale, String> getAsMap() {
        return new HashMap<>(this.localizedValues);
    }

    public boolean has(Locale locale) {
        return localizedValues.containsKey(locale);
    }

    /**
     * Put or rewrite value in default locale.
     */
    public void put(String value) {
        this.localizedValues.put(DEFAULT_LOCALE, value);
    }

    public void put(Locale locale, String value) {
        this.localizedValues.put(locale, value);
    }

    /**
     * Get value in default locale.
     */
    public String get() {
        return get(DEFAULT_LOCALE);
    }

    public String get(Locale locale) {
        return localizedValues.get(locale);
    }

    public String getClosestValue(Locale locale) {
        String value = localizedValues.get(locale);
        if (value == null) {
            value = localizedValues.values().stream().findFirst().orElse("");
        }
        return value;
    }

    @Override
    public String toString() {
        return "MLText{" + localizedValues + '}';
    }
}
