package ru.citeck.ecos.records2.meta.schema;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GqlKeyUtils {

    private static final String SPECIAL_CHARS = ":{}(),.?-+\"' ";
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("_u([A-Fa-f\\d]{4})_");

    private static String toHex(char ch) {
        String code = Integer.toHexString(ch).toUpperCase();
        return "0000".substring(code.length()) + code;
    }

    private static char toChar(String hex) {
        return (char) Integer.parseInt(hex, 16);
    }

    public static String escape(String str) {
        for (int i = 0; i < SPECIAL_CHARS.length(); i++) {
            char ch = SPECIAL_CHARS.charAt(i);
            String chStr = "" + ch;
            if (str.contains(chStr)) {
                String hexStr = toHex(ch);
                str = str.replace(chStr, "_u" + hexStr + "_");
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
