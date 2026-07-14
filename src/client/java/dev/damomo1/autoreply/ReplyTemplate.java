package dev.damomo1.autoreply;

import java.util.regex.Matcher;

public final class ReplyTemplate {
    private ReplyTemplate() {
    }

    public static String expand(String template, Matcher matcher) {
        StringBuilder result = new StringBuilder(template.length() + 16);
        for (int index = 0; index < template.length(); index++) {
            char current = template.charAt(index);
            if (current == '\\' && index + 1 < template.length()) {
                char next = template.charAt(index + 1);
                if (next == '$' || next == '\\') {
                    result.append(next);
                    index++;
                    continue;
                }
            }

            if (current != '$' || index + 1 >= template.length()) {
                result.append(current);
                continue;
            }

            char next = template.charAt(index + 1);
            if (next == '{') {
                int end = template.indexOf('}', index + 2);
                if (end < 0) {
                    throw new IllegalArgumentException("捕获组缺少右花括号 }");
                }
                String name = template.substring(index + 2, end);
                if (name.isBlank()) {
                    throw new IllegalArgumentException("捕获组名称不能为空");
                }
                appendGroup(result, matcher.group(name));
                index = end;
                continue;
            }

            if (Character.isDigit(next)) {
                int end = index + 1;
                int group = 0;
                int bestEnd = -1;
                int bestGroup = -1;
                while (end < template.length() && Character.isDigit(template.charAt(end))) {
                    int digit = template.charAt(end) - '0';
                    if (group > (Integer.MAX_VALUE - digit) / 10) {
                        break;
                    }
                    group = group * 10 + digit;
                    if (group <= matcher.groupCount()) {
                        bestGroup = group;
                        bestEnd = end;
                    }
                    end++;
                }
                if (bestGroup < 0) {
                    throw new IllegalArgumentException("不存在捕获组 $" + next);
                }
                appendGroup(result, matcher.group(bestGroup));
                index = bestEnd;
                continue;
            }

            result.append('$');
        }
        return result.toString();
    }

    private static void appendGroup(StringBuilder result, String value) {
        if (value != null) {
            result.append(value);
        }
    }
}
