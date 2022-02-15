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

    public QueueStatus() {}

    public QueueStatus(Integer prio, Integer regular, Integer total, Long timems, String msg) {
        this.prio = prio;
        this.regular = regular;
        this.total = total;
        this.timems = timems;
        this.msg = msg;
    }
}
