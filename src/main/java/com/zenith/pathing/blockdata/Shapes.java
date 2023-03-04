package com.zenith.pathing.blockdata;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collision shapes by id, each shape being composed of a list of collision boxes.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Shapes {

    @JsonIgnore
    private Map<String, List<List<Double>>> additionalProperties = new HashMap<String, List<List<Double>>>();

    @JsonAnyGetter
    public Map<String, List<List<Double>>> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, List<List<Double>> value) {
        this.additionalProperties.put(name, value);
    }

}
