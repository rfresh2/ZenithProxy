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
        var configRes = containsNullFields(CONFIG, "com.zenith");
        if (!configRes.pass()) {
            return failDueToNullsIn("config", configRes);
        }
        var launchConfigRes = containsNullFields(LAUNCH_CONFIG, "com.zenith");
        if (!launchConfigRes.pass()) {
            return failDueToNullsIn("launch_config", launchConfigRes);
        }
        return new ConfigVerificationResult(true, "");
    }

    public static ConfigVerificationResult verifyPluginConfig(Object config, String name) {
        var configRes = containsNullFields(config, config.getClass().getPackageName());
        if (!configRes.pass()) {
            return failDueToNullsIn(name, configRes);
        }
        return new ConfigVerificationResult(true, "");
    }

    public record ConfigVerificationResult(boolean pass, String msg) {}

    private static ConfigVerificationResult failDueToNullsIn(String configName, final ConfigVerificationResult configRes) {
        return new ConfigVerificationResult(false, "Null value in %s.json\n%s".formatted(configName, configRes.msg()));
    }

    private static ConfigVerificationResult containsNullFields(Object obj, String configPackagePrefix) {
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
                        return new ConfigVerificationResult(false, "'%s' in '%s' is null. Write a value manually or annotate the field with %s if intended".formatted(field.getName(), clazz.getName(), Nullable.class.getName()));
                    }
                } else {
                    if (field.getType().getName().startsWith(configPackagePrefix) && !field.getType().isEnum()) {
                        var res = containsNullFields(value, configPackagePrefix);
                        if (!res.pass()) {
                            return new ConfigVerificationResult(false, res.msg());
                        }
                    }
                }
            } catch (Throwable e) {
                DEFAULT_LOG.error("Error verifying config fields", e);
            }
        }
        return new ConfigVerificationResult(true, "");
    }

    private static boolean isNullableField(Field field) {
        return field.getAnnotatedType().getAnnotation(Nullable.class) != null;
    }
}
