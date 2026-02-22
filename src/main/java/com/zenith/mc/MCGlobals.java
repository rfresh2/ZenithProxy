package com.zenith.mc;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.smile.SmileFactory;

public final class MCGlobals {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new SmileFactory());

    private MCGlobals() {
    }
}
