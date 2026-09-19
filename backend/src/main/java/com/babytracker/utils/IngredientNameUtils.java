package com.babytracker.utils;

import com.babytracker.constants.AllergyConstants;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 食材名称解析与规范化：统一名称作为状态匹配与推荐屏蔽的唯一键 */
public final class IngredientNameUtils {

    private IngredientNameUtils() {}

    /** 从顿号/逗号/分号/空白分隔的文本解析食材 */
    public static List<String> split(String text) {
        List<String> result = new ArrayList<>();
        if (text == null) {
            return result;
        }
        for (String part : text.split(AllergyConstants.INGREDIENT_SPLIT_REGEX)) {
            String name = normalize(part);
            if (!name.isEmpty()) {
                result.add(name);
            }
        }
        return result;
    }

    /** 合并多个来源并去重，保持首次出现顺序 */
    public static List<String> mergeDistinct(List<String> names, String text) {
        Set<String> set = new LinkedHashSet<>();
        if (names != null) {
            for (String name : names) {
                String normalized = normalize(name);
                if (!normalized.isEmpty()) {
                    set.add(normalized);
                }
            }
        }
        set.addAll(split(text));
        return new ArrayList<>(set);
    }

    public static String normalize(String name) {
        return name == null ? "" : name.trim().replaceAll("\\s+", "");
    }
}
