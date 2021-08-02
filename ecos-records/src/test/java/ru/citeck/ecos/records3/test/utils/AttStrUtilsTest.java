package ru.citeck.ecos.records3.test.utils;

import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records2.meta.util.AttStrUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AttStrUtilsTest {

    @Test
    void test() {

        assertEquals(7, AttStrUtils.indexOf("'abde'cdefg", "de", 0));

        List<String> parts = new ArrayList<>(AttStrUtils.split("'abde'cdefg", "de"));
        assertEquals(new ArrayList<>(Arrays.asList("'abde'c", "fg")), parts);

        parts = new ArrayList<>(AttStrUtils.split("\"fg\"'abde'cdefg", "fg"));
        assertEquals(new ArrayList<>(Arrays.asList("\"fg\"'abde'cde", "")), parts);

        parts = new ArrayList<>(AttStrUtils.split("\"fg'abde'cdef\"g", "fg"));
        assertEquals(new ArrayList<>(Collections.singletonList("\"fg'abde'cdef\"g")), parts);

        parts = new ArrayList<>(AttStrUtils.split("\"fg'abde'c|\"g|format", "|"));
        assertEquals(new ArrayList<>(Arrays.asList("\"fg'abde'c|\"g", "format")), parts);

        parts = new ArrayList<>(AttStrUtils.split("boolNullField?bool!numberNullField?num!strNullField!'true'", "!"));
        assertEquals(new ArrayList<>(Arrays.asList("boolNullField?bool", "numberNullField?num", "strNullField", "'true'")), parts);

        assertEquals(-1, AttStrUtils.indexOf("", "|"));
        assertEquals(-1, AttStrUtils.indexOf("abc", "abcdefg"));
        assertEquals(0, AttStrUtils.indexOf("abc", "abc"));
        assertEquals(-1, AttStrUtils.indexOf("'abc", "abc"));
        assertEquals(-1, AttStrUtils.indexOf("\"abc", "abc"));

        assertEquals(new AttStrUtils.SplitPair("ab", "cde|fg"), AttStrUtils.splitByFirst("ab|cde|fg", "|"));
        assertEquals(new AttStrUtils.SplitPair("ab\\|cde", "fg"), AttStrUtils.splitByFirst("ab\\|cde|fg", "|"));
        assertEquals("ab|cde", AttStrUtils.removeEscaping("ab\\|cde"));

        assertEquals(new AttStrUtils.SplitPair("a{b|}cde", "fg"), AttStrUtils.splitByFirst("a{b|}cde|fg", "|"));
        assertEquals(new AttStrUtils.SplitPair("a{b|}c(de|fg)", ""), AttStrUtils.splitByFirst("a{b|}c(de|fg)", "|"));
        assertEquals(new AttStrUtils.SplitPair("a", "b|}c(de|fg)"), AttStrUtils.splitByFirst("a{b|}c(de|fg)", "{"));
        assertEquals(new AttStrUtils.SplitPair("a{b|}cde", "fg)"), AttStrUtils.splitByFirst("a{b|}cde|fg)", "|"));

        assertEquals(new AttStrUtils.SplitPair("abc", "'def'"), AttStrUtils.splitByFirst("abc|'def'", "|"));
    }

    @Test
    void indexOfTest() {
        assertThat(AttStrUtils.indexOf("some[]att", "[]")).isEqualTo(4);
    }

    @Test
    void replaceTest() {

        assertEquals("one two three one two three".replace("one", "replaced"), AttStrUtils.replace("one two three one two three", "one", "replaced"));
        assertEquals("onetwo\\.th(ree.third)", AttStrUtils.replace("one.two\\.th(ree.third)", ".", ""));
        assertEquals("one.two_th(ree.third)", AttStrUtils.replace("one.two\\.th(ree.third)", "\\.", "_"));
        assertEquals("'one.two\\.th(ree.third)'", AttStrUtils.replace("'one.two\\.th(ree.third)'", "\\.", "_"));
    }

    @Test
    void testRemEsc() {
        assertEquals("\\", AttStrUtils.removeEscaping("\\\\"));
        assertEquals("abc", AttStrUtils.removeEscaping("\\abc"));
    }

    @Test
    void testEscape() {
        List<String> split = AttStrUtils.split("abc\\.cde.efg", ".");
        assertEquals(2, split.size());
        assertEquals("abc\\.cde", split.get(0));
        assertEquals("efg", split.get(1));
    }
}
