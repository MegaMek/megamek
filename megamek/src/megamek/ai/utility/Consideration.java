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
import megamek.client.bot.caspar.ai.utility.tw.considerations.*;

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
    @JsonSubTypes.Type(value = BackSide.class, name = "BackSide"),
    @JsonSubTypes.Type(value = CoverFire.class, name = "CoverFire"),
    @JsonSubTypes.Type(value = CrowdingEnemies.class, name = "CrowdingEnemies"),
    @JsonSubTypes.Type(value = CrowdingFriends.class, name = "CrowdingFriends"),
    @JsonSubTypes.Type(value = CurrentThreat.class, name = "CurrentThreat"),
    @JsonSubTypes.Type(value = DamageOutput.class, name = "DamageOutput"),
    @JsonSubTypes.Type(value = DecoyValue.class, name = "DecoyValue"),
    @JsonSubTypes.Type(value = ECMCoverage.class, name = "ECMCoverage"),
    @JsonSubTypes.Type(value = EnemyECMCoverage.class, name = "EnemyECMCoverage"),
    @JsonSubTypes.Type(value = EnemyPositioning.class, name = "EnemyPositioning"),
    @JsonSubTypes.Type(value = EnvironmentalCover.class, name = "EnvironmentalCover"),
    @JsonSubTypes.Type(value = EnvironmentalHazard.class, name = "EnvironmentalHazard"),
    @JsonSubTypes.Type(value = FacingTheEnemy.class, name = "FacingTheEnemy"),
    @JsonSubTypes.Type(value = FavoriteTargetInRange.class, name = "FavoriteTargetInRange"),
    @JsonSubTypes.Type(value = FireExposure.class, name = "FireExposure"),
    @JsonSubTypes.Type(value = FlankingPosition.class, name = "FlankingPosition"),
    @JsonSubTypes.Type(value = FormationCohesion.class, name = "FormationCohesion"),
    @JsonSubTypes.Type(value = FriendlyArtilleryFire.class, name = "FriendlyArtilleryFire"),
    @JsonSubTypes.Type(value = FriendlyPositioning.class, name = "FriendlyPositioning"),
    @JsonSubTypes.Type(value = FriendsCoverFire.class, name = "FriendsCoverFire"),
    @JsonSubTypes.Type(value = FrontSide.class, name = "FrontSide"),
    @JsonSubTypes.Type(value = HeatVulnerability.class, name = "HeatVulnerability"),
    @JsonSubTypes.Type(value = IsVIPCloser.class, name = "IsVIPCloser"),
    @JsonSubTypes.Type(value = KeepDistance.class, name = "KeepDistance"),
    @JsonSubTypes.Type(value = LeftSide.class, name = "LeftSide"),
    @JsonSubTypes.Type(value = MyUnitBotSettings.class, name = "MyUnitBotSettings"),
    @JsonSubTypes.Type(value = MyUnitHeatManagement.class, name = "MyUnitHeatManagement"),
    @JsonSubTypes.Type(value = MyUnitIsCrippled.class, name = "MyUnitIsCrippled"),
    @JsonSubTypes.Type(value = MyUnitIsMovingTowardsWaypoint.class, name = "MyUnitIsMovingTowardsWaypoint"),
    @JsonSubTypes.Type(value = MyUnitMoved.class, name = "MyUnitMoved"),
    @JsonSubTypes.Type(value = MyUnitRoleIs.class, name = "MyUnitRoleIs"),
    @JsonSubTypes.Type(value = MyUnitTMM.class, name = "MyUnitTMM"),
    @JsonSubTypes.Type(value = MyUnitUnderThreat.class, name = "MyUnitUnderThreat"),
    @JsonSubTypes.Type(value = OverallArmor.class, name = "OverallArmor"),
    @JsonSubTypes.Type(value = PilotingCaution.class, name = "PilotingCaution"),
    @JsonSubTypes.Type(value = Retreat.class, name = "Retreat"),
    @JsonSubTypes.Type(value = RightSide.class, name = "RightSide"),
    @JsonSubTypes.Type(value = Scouting.class, name = "Scouting"),
    @JsonSubTypes.Type(value = StandStill.class, name = "StandStill"),
    @JsonSubTypes.Type(value = StrategicGoal.class, name = "StrategicGoal"),
    @JsonSubTypes.Type(value = TargetUnitsArmor.class, name = "TargetUnitsArmor"),
    @JsonSubTypes.Type(value = TargetWithinOptimalRange.class, name = "TargetWithinOptimalRange"),
    @JsonSubTypes.Type(value = TargetWithinRange.class, name = "TargetWithinRange"),
    @JsonSubTypes.Type(value = TurnsToEncounter.class, name = "TurnsToEncounter"),
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Consideration implements NamedObject {
    @JsonProperty("name")
    private String name;
    @JsonProperty("curve")
    private Curve curve;
    @JsonProperty("parameters")
    protected Map<String, Object> parameters = new HashMap<>();

    public Consideration() {
    }

    public Consideration(String name) {
        this.name = name;
    }

    public Consideration(String name, Curve curve) {
        this(name, curve, new HashMap<>(4));
    }

    public Consideration(String name, Curve curve, Map<String, Object> parameters) {
        this.name = name;
        this.curve = curve;
        this.parameters = new HashMap<>();
        for (String key : parameters.keySet()) {
            if (getParameterTypes().containsKey(key)) {
                this.parameters.put(key, parameters.get(key));
            } else {
                throw new IllegalArgumentException("Unknown parameter: " + key);
            }
        }

    }

    @JsonIgnore
    public Map<String, Class<?>> getParameterTypes() {
        return Collections.emptyMap();
    }

    @JsonIgnore
    public Map<String, ParameterTitleTooltip> getParameterTooltips() {
        return Collections.emptyMap();
    }

    public abstract double score(DecisionContext context);

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
                // noinspection unchecked
                var enumValues = ((Class<? extends Enum<?>>) clazz).getEnumConstants();
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

    public abstract Consideration copy();

    @Override
    public String toString() {
        return new StringJoiner(", ", Consideration.class.getSimpleName() + " [", "]")
            .add("name='" + name + "'")
            .add("curve=" + curve)
            .add("parameters=" + parameters)
            .toString();
    }
}
