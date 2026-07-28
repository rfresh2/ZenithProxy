package com.zenith.feature.api.mcsrvstatus.model;

import java.util.List;

public record MCSrvStatusMotdData(
    List<String> raw,
    List<String> clean,
    List<String> html
) {
}
