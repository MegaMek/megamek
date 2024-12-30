/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.ai.utility;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import megamek.client.bot.duchess.ai.utility.tw.considerations.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = TWConsideration.class, name = "TWConsideration"),
    @JsonSubTypes.Type(value = MyUnitArmor.class, name = "MyUnitArmor"),
    @JsonSubTypes.Type(value = TargetWithinOptimalRange.class, name = "TargetWithinOptimalRange"),
    @JsonSubTypes.Type(value = TargetWithinRange.class, name = "TargetWithinRange"),
    @JsonSubTypes.Type(value = MyUnitUnderThreat.class, name = "MyUnitUnderThreat"),
    @JsonSubTypes.Type(value = MyUnitRoleIs.class, name = "MyUnitRoleIs"),
    @JsonSubTypes.Type(value = TargetUnitsHaveRole.class, name = "TargetUnitsHaveRole"),
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Consideration<IN_GAME_OBJECT,TARGETABLE>  implements NamedObject {
    @JsonProperty("name")
    private String name;
    @JsonProperty("curve")
    private Curve curve;
    @JsonProperty("parameters")
    protected Map<String, Object> parameters = Collections.emptyMap();

    protected transient Map<String, Class<?>> parameterTypes = Collections.emptyMap();

    public Consideration() {
    }

    public Consideration(String name) {
        this.name = name;
    }

    public Consideration(String name, Curve curve) {
        this(name, curve, Collections.emptyMap());
    }

    public Consideration(String name, Curve curve, Map<String, Object> parameters) {
        this.name = name;
        this.curve = curve;
        this.parameters = Map.copyOf(parameters);
    }

    public abstract double score(DecisionContext<IN_GAME_OBJECT, TARGETABLE> context);

    public Curve getCurve() {
        return curve;
    }

    public void setCurve(Curve curve) {
        this.curve = curve;
    }

    public Map<String, Object> getParameters() {
        return Map.copyOf(parameters);
    }

    public Map<String, Class<?>> getParameterTypes() {
        return Map.copyOf(parameterTypes);
    }

    public Class<?> getParameterType(String key) {
        return parameterTypes.get(key);
    }

    public void setParameters(Map<String, Object> parameters) {
        var params = new HashMap<String, Object>();
        for (var entry : parameters.entrySet()) {
            var clazz = parameterTypes.get(entry.getKey());
            if (clazz == null) {
                throw new IllegalArgumentException("Unknown parameter: " + entry.getKey());
            }
            if (clazz.isAssignableFrom(entry.getValue().getClass())) {
                throw new IllegalArgumentException("Invalid parameter type for " + entry.getKey() + ": " + entry.getValue().getClass());
            }
            params.put(entry.getKey(), entry.getValue());
        }
        this.parameters = Map.copyOf(parameters);
    }

    public double getDoubleParameter(String key) {
        return (double) getParameter(key);
    }

    public int getIntParameter(String key) {
        return (int) getParameter(key);
    }

    public boolean getBooleanParameter(String key) {
        return (boolean) getParameter(key);
    }

    public String getStringParameter(String key) {
        return (String) getParameter(key);
    }

    public float getFloatParameter(String key) {
        return (float) getParameter(key);
    }

    public long getLongParameter(String key) {
        return (long) getParameter(key);
    }

    public Object getParameter(String key) {
        return parameters.get(key);
    }

    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }

    public double computeResponseCurve(double score) {
        return curve.evaluate(score);
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Consideration.class.getSimpleName() + " [", "]")
            .add("name='" + name + "'")
            .add("curve=" + curve)
            .add("parameters=" + parameters)
            .toString();
    }
}
