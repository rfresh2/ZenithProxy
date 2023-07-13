package com.zenith.feature.pathing.blockdata;

import com.fasterxml.jackson.annotation.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "name",
        "type",
        "values",
        "num_values"
})
public class State {

    /**
     * The name of the property
     * (Required)
     */
    @JsonProperty("name")
    @JsonPropertyDescription("The name of the property")
    private String name;
    /**
     * The type of the property
     * (Required)
     */
    @JsonProperty("type")
    @JsonPropertyDescription("The type of the property")
    private State.Type type;
    /**
     * The possible values of the property
     */
    @JsonProperty("values")
    @JsonPropertyDescription("The possible values of the property")
    private List<Object> values = null;
    /**
     * The number of possible values
     * (Required)
     */
    @JsonProperty("num_values")
    @JsonPropertyDescription("The number of possible values")
    private Double numValues;

    /**
     * The name of the property
     * (Required)
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * The name of the property
     * (Required)
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * The type of the property
     * (Required)
     */
    @JsonProperty("type")
    public State.Type getType() {
        return type;
    }

    /**
     * The type of the property
     * (Required)
     */
    @JsonProperty("type")
    public void setType(State.Type type) {
        this.type = type;
    }

    /**
     * The possible values of the property
     */
    @JsonProperty("values")
    public List<Object> getValues() {
        return values;
    }

    /**
     * The possible values of the property
     */
    @JsonProperty("values")
    public void setValues(List<Object> values) {
        this.values = values;
    }

    /**
     * The number of possible values
     * (Required)
     */
    @JsonProperty("num_values")
    public Double getNumValues() {
        return numValues;
    }

    /**
     * The number of possible values
     * (Required)
     */
    @JsonProperty("num_values")
    public void setNumValues(Double numValues) {
        this.numValues = numValues;
    }


    /**
     * The type of the property
     */
    public enum Type {

        ENUM("enum"),
        BOOL("bool"),
        INT("int");
        private final static Map<String, Type> CONSTANTS = new Object2ObjectOpenHashMap<>();

        static {
            for (State.Type c : values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private final String value;

        Type(String value) {
            this.value = value;
        }

        @JsonCreator
        public static State.Type fromValue(String value) {
            State.Type constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

    }

}
