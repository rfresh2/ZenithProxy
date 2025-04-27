package com.zenith.util;

import lombok.SneakyThrows;

public class KotlinUtil {
    public static boolean isKotlinObject(Class<?> clazz) {
        if (!isKotlinClass(clazz)) return false;
        var declaredFields = clazz.getDeclaredFields();
        for (int i = 0; i < declaredFields.length; i++) {
            var field = declaredFields[i];
            if (field.getName().equals("INSTANCE") && field.getType().equals(clazz)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isKotlinClass(Class<?> clazz) {
        var declaredAnnotations = clazz.getDeclaredAnnotations();
        for (int i = 0; i < declaredAnnotations.length; i++) {
            var annotation = declaredAnnotations[i];
            if (annotation.annotationType().getName().contains("kotlin.Metadata")) {
                return true;
            }
        }
        return false;
    }

    @SneakyThrows
    public static <T> T getKotlinObject(Class<T> clazz) {
        return (T) clazz.getDeclaredField("INSTANCE").get(null);
    }
}
