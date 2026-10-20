package com.zenith.util.config;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;

import static com.zenith.Globals.*;
import static java.lang.reflect.Modifier.isStatic;

// Verifies that all fields in the loaded configs are not null
// gson will deserialize invalid json values to null
// but this will cause issues as we expect all fields to be non-null
public final class ConfigVerifier {
    private ConfigVerifier() {}

    public static ConfigVerificationResult verifyConfigs() {
        if (containsNullFields(CONFIG, "com.zenith")) {
            return failDueToNullsIn("config");
        }
        if (containsNullFields(LAUNCH_CONFIG, "com.zenith")) {
            return failDueToNullsIn("launch_config");
        }
        return new ConfigVerificationResult(true, "");
    }

    public static ConfigVerificationResult verifyPluginConfig(Object config, String name) {
        if (containsNullFields(config, config.getClass().getPackageName())) {
            return failDueToNullsIn(name);
        }
        return new ConfigVerificationResult(true, "");
    }

    public record ConfigVerificationResult(boolean pass, String msg) {}

    private static ConfigVerificationResult failDueToNullsIn(String configName) {
        return new ConfigVerificationResult(false, "Null value in %s.json".formatted(configName));
    }

    private static boolean containsNullFields(Object obj, String configPackagePrefix) {
        var clazz = obj.getClass();
        // recursively check all fields for null
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            try {
                if (field.getType().isPrimitive()) {
                    continue;
                }
                // skip static fields
                if (isStatic(field.getModifiers())) continue;
                // skip non-accessible fields
                if (!field.canAccess(obj)) continue;
                Object value = field.get(obj);
                if (value == null) {
                    if (!isNullableField(field)) {
                        DEFAULT_LOG.error("Field: '{}' in '{}' is null\nIf this is intended, annotate the field with: {}", field.getName(), clazz.getName(), Nullable.class.getName());
                        return true;
                    }
                } else {
                    if (field.getType().getName().startsWith(configPackagePrefix) && !field.getType().isEnum()) {
                        if (containsNullFields(value, configPackagePrefix)) {
                            return true;
                        }
                    }
                }
            } catch (Throwable e) {
                DEFAULT_LOG.error("Error verifying config fields", e);
            }
        }
        return false;
    }

    private static boolean isNullableField(Field field) {
        return field.getAnnotatedType().getAnnotation(Nullable.class) != null;
    }
}
