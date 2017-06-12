package net.daporkchop.toobeetooteebot.util;

import java.io.OutputStream;
import java.io.PrintStream;

public class AdvancedOutputStream extends PrintStream {
    public PrintStream original;

    public AdvancedOutputStream()   {
        super(System.out);
        original = System.out;
    }

    public void println() {

    }
}
