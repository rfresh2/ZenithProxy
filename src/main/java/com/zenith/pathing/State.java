package com.zenith.pathing;

import lombok.Data;

import java.util.List;

@Data
public class State {
    private final String name;
    private final String type;
    private final List<String> values;
    private final Integer num_values;
}
