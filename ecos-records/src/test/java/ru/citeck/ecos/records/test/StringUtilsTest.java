package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records2.utils.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringUtilsTest {

    @Test
    void test() {

        String result = StringUtils.escapeDoubleQuotes("abc\"de");
        assertEquals("abc\\\"de", result);

        result = StringUtils.escapeDoubleQuotes("abc\\\"de");
        assertEquals("abc\\\"de", result);

        result = StringUtils.escapeDoubleQuotes("abc\\\"de\"");
        assertEquals("abc\\\"de\\\"", result);

        result = StringUtils.escapeDoubleQuotes("\"abc");
        assertEquals("\\\"abc", result);

        result = StringUtils.escapeDoubleQuotes("\\\"abc");
        assertEquals("\\\"abc", result);

        result = StringUtils.escapeDoubleQuotes("\\\\abc");
        assertEquals("\\\\abc", result);
    }
}
