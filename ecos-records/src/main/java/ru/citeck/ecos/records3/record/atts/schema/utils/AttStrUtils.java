package ru.citeck.ecos.records3.record.atts.schema.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class AttStrUtils {

    public static String removeQuotes(String str) {
        if (isInQuotes(str)) {
            String result = str.substring(1, str.length() - 1);
            if (str.charAt(0) == '\'') {
                result = result.replace("\\'", "'");
            } else {
                result = result.replace("\\\"", "\"");
            }
            return result;
        }
        return str;
    }

    public static boolean isInQuotes(String str) {
        if (str == null || str.length() < 2) {
            return false;
        }
        char ch = str.charAt(0);
        int lastIdx = str.length() - 1;
        if ((ch != '"' && ch != '\'') || str.charAt(lastIdx) != ch) {
            return false;
        }
        int idx = str.indexOf(ch, 1);
        while (idx != -1 && idx != lastIdx && isEscapedChar(str, idx)) {
            idx = str.indexOf(ch, idx + 1);
        }
        return idx == lastIdx;
    }

    public static String removeEscaping(String str) {

        if (str == null || !str.contains("\\")) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str.length());
        int idx = 0;
        for (; idx < str.length() - 1; idx++) {
            char ch = str.charAt(idx);
            if (ch == '\\') {
                sb.append(str.charAt(idx + 1));
                idx++;
            } else {
                sb.append(ch);
            }
        }
        if (idx < str.length()) {
            sb.append(str.charAt(idx));
        }
        return sb.toString();
    }

    public static SplitPair splitByFirst(String str, String delim) {
        int idx = indexOf(str, delim);
        if (idx == -1) {
            return new SplitPair(str, "");
        }
        return new SplitPair(str.substring(0, idx), str.substring(idx + delim.length()));
    }

    public static List<String> splitAndSkipFirst(String str, char delim) {
        List<String> splitRes = split(str, delim);
        if (splitRes.size() <= 1) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (int i = 1; i < splitRes.size(); i++) {
            result.add(splitRes.get(i));
        }
        return result;
    }

    public static String replace(String str, String sub, String target) {

        int idx = indexOf(str, sub);
        if (idx == -1) {
            return str;
        }

        int prevIdx = 0;
        StringBuilder sb = new StringBuilder();

        while (idx > -1) {

            sb.append(str, prevIdx, idx);
            sb.append(target);
            prevIdx = idx + sub.length();

            idx = indexOf(str, sub, prevIdx);
        }
        if (prevIdx < str.length()) {
            sb.append(str.substring(prevIdx));
        }

        return sb.toString();
    }

    public static List<String> split(String str, char delim) {
        return split(str, String.valueOf(delim));
    }

    public static List<String> split(String str, String delim) {

        int prevIdx = 0;
        int idx = indexOf(str, delim, prevIdx);

        List<String> result = new ArrayList<>();
        while (idx != -1) {
            result.add(str.substring(prevIdx, idx));
            prevIdx = idx + delim.length();
            idx = indexOf(str, delim, prevIdx);
        }
        result.add(str.substring(prevIdx));

        return result;
    }

    public static boolean hasCharBeforeIdx(String str, int idx, Set<Character> charsToCheck) {
        if (charsToCheck.isEmpty()) {
            return false;
        }
        for (Character ch : charsToCheck) {
            int chIdx = indexOf(str, ch);
            if (chIdx != -1 && chIdx < idx) {
                return true;
            }
        }
        return false;
    }

    public static int indexOf(String str, char ch) {
        return indexOf(str, String.valueOf(ch));
    }

    public static int indexOf(String str, String subString) {
        return indexOf(str, subString, 0);
    }

    public static int indexOf(String str, String subString, int fromIdx) {

        if (subString.length() > 1 && hasOpenContextChar(subString) && !subString.equals("[]")) {
            return -1;
        }

        char openContextChar = ' ';
        int openCounter = 0;
        for (int idx = fromIdx; idx <= (str.length() - subString.length()); idx++) {

            char currentChar = str.charAt(idx);
            if (openContextChar != ' ') {
                if (currentChar == openContextChar
                    && (currentChar == '(' || currentChar == '{' || currentChar == '[')) {

                    openCounter++;
                }
                if (isCloseContextChar(openContextChar, currentChar)) {
                    int slashes = 0;
                    int backIdx = idx;
                    while (--backIdx > 0 && str.charAt(backIdx) == '\\') {
                        slashes++;
                    }
                    if (slashes % 2 == 1) {
                        continue;
                    }
                    if (--openCounter == 0) {
                        openContextChar = ' ';
                        if (containsAt(str, idx, subString)) {
                            return idx;
                        }
                    }
                }
                continue;
            }
            if (containsAt(str, idx, subString)) {
                return idx;
            } else if (isOpenContextChar(currentChar)) {
                openContextChar = currentChar;
                openCounter++;
            }
        }
        return -1;
    }

    public static int getLastNonWhitespaceCharIdx(String str) {
        if (str.isEmpty()) {
            return -1;
        }
        int idx = str.length() - 1;
        while (idx >= 0 && Character.isWhitespace(str.charAt(idx))) {
            idx--;
        }
        return idx;
    }

    private static boolean containsAt(String str, int idx, String subString) {
        if (str.length() < idx + subString.length()) {
            return false;
        }
        if (subString.length() == 1 && idx > 0 && str.charAt(idx - 1) == '\\') {
            return false;
        }
        for (int i = 0; i < subString.length(); i++) {
            if (str.charAt(idx + i) != subString.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasOpenContextChar(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (isOpenContextChar(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOpenContextChar(char ch) {
        return ch == '\'' || ch == '"' || ch == '(' || ch == '{' || ch == '[';
    }

    private static boolean isCloseContextChar(char openChar, char ch) {
        if (openChar == '\'' || openChar == '"') {
            return ch == openChar;
        }
        if (openChar == '(') {
            return ch == ')';
        }
        if (openChar == '{') {
            return ch == '}';
        }
        if (openChar == '[') {
            return ch == ']';
        }
        return false;
    }

    private static boolean isEscapedChar(String value, int idx) {
        if (idx < 0 || idx >= value.length()) {
            return false;
        }
        int backSlashesCount = 0;
        int backSlashesIdx = idx;
        while (backSlashesIdx > 0 && value.charAt(backSlashesIdx - 1) == '\\') {
            backSlashesCount++;
            backSlashesIdx--;
        }
        return backSlashesCount % 2 == 1;
    }

    @Data
    @AllArgsConstructor
    public static class SplitPair {
        private String first;
        private String second;
    }
}
