package com.zenith.testfixtures;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

public final class SharedLoaderFixtures {
    private SharedLoaderFixtures() { }

    public static final class PluginClass {
        public String value() {
            return "plugin";
        }
    }

    public static final class FirstProvider implements Runnable {
        @Override
        public void run() { }
    }

    public static final class SecondProvider implements Runnable {
        @Override
        public void run() { }
    }

    public static final class TransformTarget {
        public static String value() {
            return "original";
        }
    }

    @Mixin(TransformTarget.class)
    public static final class TransformMixin {
        @Overwrite
        public static String value() {
            return "patched!";
        }
    }
}
