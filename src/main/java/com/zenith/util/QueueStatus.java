package com.zenith.util;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QueueStatus {
    @JsonProperty("prio")
    public Integer prio;
    @JsonProperty("regular")
    public Integer regular;
    @JsonProperty("total")
    public Integer total;
    @JsonProperty("timems")
    public Long timems;
    @JsonProperty("msg")
    public String msg;
}
