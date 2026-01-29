package com.zenith.plugin.api;

import org.intellij.lang.annotations.Pattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Plugin {
    @Pattern(PluginInfo.ID_PATTERN_STRING)
    String id();
    @Pattern(Version.VERSION_PATTERN_STRING)
    String version() default "";
    String description() default "";
    String url() default "";
    String[] authors() default "";
    String[] mcVersions() default "*";
}
