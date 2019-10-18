package ru.citeck.ecos.records2.utils;

public class ObjectKeyGenerator {

    private static final String CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CHARS_LEN = CHARS.length();

    private int numValue = -1;
    private StringBuilder value = new StringBuilder();

    public String incrementAndGet() {

        value.setLength(0);

        int left = ++numValue;
        do {
            value.append(CHARS.charAt(left % CHARS_LEN));
            left /= CHARS_LEN;
        } while (left > 0);

        return value.toString();
    }
}
