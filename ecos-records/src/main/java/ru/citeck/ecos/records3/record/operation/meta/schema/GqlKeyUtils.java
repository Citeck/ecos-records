package ru.citeck.ecos.records3.record.operation.meta.schema;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GqlKeyUtils {

    private static final char[] SPECIAL_CHARS = "@|:{}(),.!?$-+\"' ".toCharArray();
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("_u([A-Fa-f\\d]{4})_");

    private static final String[] SPECIAL_CHARS_HEX;

    static {
        Function<Character, String> toHex = ch -> {
            String code = Integer.toHexString(ch).toUpperCase();
            return "_u" + "0000".substring(code.length()) + code + "_";
        };

        SPECIAL_CHARS_HEX = new String[SPECIAL_CHARS.length];
        for (int i = 0; i < SPECIAL_CHARS.length; i++) {
            SPECIAL_CHARS_HEX[i] = toHex.apply(SPECIAL_CHARS[i]);
        }
    }

    private static char toChar(String hex) {
        return (char) Integer.parseInt(hex, 16);
    }

    public static String escape(String str) {
        for (int i = 0; i < SPECIAL_CHARS.length; i++) {
            char ch = SPECIAL_CHARS[i];
            String chStr = "" + ch;
            if (str.contains(chStr)) {
                str = str.replace(chStr, SPECIAL_CHARS_HEX[i]);
            }
        }
        return str;
    }

    public static String unescape(String str) {

        Matcher matcher = SPECIAL_CHAR_PATTERN.matcher(str);
        while (matcher.find()) {
            str = str.replace(matcher.group(0), toChar(matcher.group(1)) + "");
        }
        return str;
    }
}
