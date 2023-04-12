package com.zenith.util.log;

import ch.qos.logback.core.joran.spi.NoAutoStart;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;

import java.io.File;

@NoAutoStart
public class StartupSizeAndTimeBasedTriggeringPolicy<E> extends SizeAndTimeBasedFNATP<E> {
    private boolean started = false;

    @Override
    public boolean isTriggeringEvent(File activeFile, E event) {
        if (!started) {
            atomicNextCheck.set(0L);
            return started = true;
        }

        return super.isTriggeringEvent(activeFile, event);
    }
}
