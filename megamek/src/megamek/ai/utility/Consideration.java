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

import com.fasterxml.jackson.annotation.*;
import megamek.client.bot.queen.ai.utility.tw.considerations.*;

import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = TWConsideration.class, name = "TWConsideration"),
    @JsonSubTypes.Type(value = DamageOutput.class, name = "DamageOutput"),
    @JsonSubTypes.Type(value = FacingTheEnemy.class, name = "FacingTheEnemy"),
    @JsonSubTypes.Type(value = FavoriteTargetInRange.class, name = "FavoriteTargetInRange"),
    @JsonSubTypes.Type(value = IsVIPCloser.class, name = "IsVIPCloser"),
    @JsonSubTypes.Type(value = MyUnitArmor.class, name = "MyUnitArmor"),
    @JsonSubTypes.Type(value = MyUnitBotSettings.class, name = "MyUnitBotSettings"),
    @JsonSubTypes.Type(value = MyUnitHeatManagement.class, name = "MyUnitHeatManagement"),
    @JsonSubTypes.Type(value = MyUnitIsCrippled.class, name = "MyUnitIsCrippled"),
    @JsonSubTypes.Type(value = MyUnitIsMovingTowardsWaypoint.class, name = "MyUnitIsMovingTowardsWaypoint"),
    @JsonSubTypes.Type(value = TargetUnitsArmor.class, name = "TargetUnitsArmor"),
    @JsonSubTypes.Type(value = TargetWithinOptimalRange.class, name = "TargetWithinOptimalRange"),
    @JsonSubTypes.Type(value = TargetWithinRange.class, name = "TargetWithinRange")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Consideration<IN_GAME_OBJECT,TARGETABLE>  implements NamedObject {
    @JsonProperty("name")
    private String name;
    @JsonProperty("curve")
    private Curve curve;
    @JsonProperty("parameters")
    protected Map<String, Object> parameters = Collections.emptyMap();

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

    @JsonIgnore
    public Map<String, Class<?>> getParameterTypes() {
        return Collections.emptyMap();
    }

    @JsonIgnore
    public Map<String, ParameterTitleTooltip> getParameterTooltips() {
        return Collections.emptyMap();
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

    public void setParameters(Map<String, Object> parameters) {
        for (var entry : parameters.entrySet()) {
            var clazz = getParameterTypes().get(entry.getKey());
            if (clazz == null) {
                throw new IllegalArgumentException("Unknown parameter: " + entry.getKey());
            }
            if (clazz.isEnum() && entry.getValue() instanceof String value) {
                var enumValues = ((Class<? extends Enum>) clazz).getEnumConstants();
                for (var anEnum : enumValues) {
                    if (anEnum.toString().equalsIgnoreCase(value)) {
                        parameters.put(entry.getKey(), anEnum);
                        break;
                    }
                }
            } else if (!clazz.isAssignableFrom(entry.getValue().getClass())) {
                throw new IllegalArgumentException("Invalid parameter type for " + entry.getKey() + ": " + entry.getValue().getClass());
            }
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

    public <T> T getParameter(String key, Class<T> clazz) {
        return clazz.cast(parameters.get(key));
    }

    public boolean containsParameter(String key) {
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
