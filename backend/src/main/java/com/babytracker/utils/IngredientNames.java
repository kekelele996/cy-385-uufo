package com.babytracker.utils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 食材名称规范化：存储用规范形式（去空白），匹配用键（小写），保证同一食材不同写法可正确归并。
 */
public final class IngredientNames {

    private IngredientNames() {}

    /** 存储/展示用：去除首尾及折叠内部空白 */
    public static String canonical(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("\\s+", " ");
    }

    /** 匹配用：在规范形式基础上转小写 */
    public static String matchKey(String raw) {
        return canonical(raw).toLowerCase(Locale.ROOT);
    }

    /**
     * 解析食谱 ingredients / allergens 文本：支持顿号、逗号（中英文）、斜杠、分号分隔
     */
    public static List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String[] tokens = text.split("[、,，/;；]");
        Set<String> result = new LinkedHashSet<>();
        for (String token : tokens) {
            String name = canonical(token);
            if (!name.isEmpty()) {
                result.add(name);
            }
        }
        return List.copyOf(result);
    }

    /**
     * 规范化一餐提交的食材：去空白、按匹配键去重，保留首次出现的写法
     */
    public static List<String> normalizeBatch(List<String> rawList) {
        if (rawList == null) {
            return List.of();
        }
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String raw : rawList) {
            String name = canonical(raw);
            if (!name.isEmpty()) {
                seen.add(name);
            }
        }
        return Arrays.asList(seen.toArray(new String[0]));
    }

    /**
     * 以匹配键判断两个食材名是否指向同一食材
     */
    public static boolean sameIngredient(String a, String b) {
        return matchKey(a).equals(matchKey(b));
    }
}
