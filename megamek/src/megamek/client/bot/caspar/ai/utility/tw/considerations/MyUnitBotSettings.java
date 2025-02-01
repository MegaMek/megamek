/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.client.bot.caspar.ai.utility.tw.considerations;

import com.fasterxml.jackson.annotation.JsonTypeName;
import megamek.ai.utility.DecisionContext;
import megamek.ai.utility.ParameterTitleTooltip;
import megamek.client.bot.caspar.ai.utility.tw.decision.TWDecisionContext;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Infantry;

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * Decides that it is too dangerous to stay under enemy weapons range.
 */
@JsonTypeName("MyUnitBotSettings")
public class MyUnitBotSettings extends TWConsideration {

    private static final String herdingWeight = "herding weight";
    private static final String braveryWeight = "bravery weight";
    private static final String aggressionWeight = "aggression weight";
    private static final String cautionWeight = "caution weight";
    private static final String selfPreservationWeight = "self preservation weight";

    private static final Map<String, Class<?>> parameterTypes = Map.of(
        herdingWeight, Double.class,
        braveryWeight, Double.class,
        aggressionWeight, Double.class,
        cautionWeight, Double.class,
        selfPreservationWeight, Double.class);

    private static final Map<String, ParameterTitleTooltip> parameterTooltips = Map.of(
        herdingWeight, new ParameterTitleTooltip("HerdingWeight"),
        braveryWeight, new ParameterTitleTooltip("BraveryWeight"),
        aggressionWeight, new ParameterTitleTooltip("AggressionWeight"),
        cautionWeight, new ParameterTitleTooltip("CautionWeight"),
        selfPreservationWeight, new ParameterTitleTooltip("SelfPreservationWeight"));

    public MyUnitBotSettings() {
        parameters = Map.of(
            herdingWeight, 1.0,
            braveryWeight, 1.0,
            aggressionWeight, 1.0,
            cautionWeight, 1.0,
            selfPreservationWeight, 1.0
        );
    }

    @Override
    public Map<String, Class<?>> getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public Map<String, ParameterTitleTooltip> getParameterTooltips() {
        return parameterTooltips;
    }

    @Override
    public double score(DecisionContext<Entity, Entity> context) {
        TWDecisionContext twContext = (TWDecisionContext) context;
        var score = herding(twContext) * bravery(twContext) * aggression(twContext) * caution(twContext) * selfPreservation(twContext);
        return clamp01(score);
    }

    private double herding(TWDecisionContext twContext) {
        var herdingMod = getDoubleParameter(herdingWeight);
        if (herdingMod == 0.0) {
            return 1;
        }
        var self = twContext.getCurrentUnit();
        var clusterCentroid = twContext.getFriendsClusterCentroid(self);
        double distToFriends = clusterCentroid.distance(self.getPosition());

        double herding = twContext.getBehaviorSettings().getHerdMentalityIndex();
        double herdingFraction = herding / 10.0;
        double closeness = 1.0 / (1.0 + distToFriends);
        herdingMod *= herdingFraction * closeness;
        return clamp01(herdingMod);
    }

    private double bravery(TWDecisionContext twContext) {
        var braveryMod = getDoubleParameter(braveryWeight);
        if (braveryMod == 0) {
            return 1.0;
        }
        var self = twContext.getCurrentUnit();
        var myWeaponsDamage = Compute.computeTotalDamage(self.getTotalWeaponList());
        var totalDamageFraction = clamp01(twContext.getTotalDamage() / (double) myWeaponsDamage);
        var damageCap = clamp01(twContext.getExpectedDamage() / (double) self.getTotalArmor()) / 2;
        double braveryValue = twContext.getBehaviorSettings().getBraveryIndex();
        double braveryFraction = braveryValue / 10.0;
        braveryMod *= totalDamageFraction * braveryFraction - (1 - damageCap);
        return clamp01(braveryMod);
    }

    /**
     * Aggression is a measure of how much the unit wants to attack.
     */
    public double aggression(TWDecisionContext twContext) {
        var aggressionMod = getDoubleParameter(aggressionWeight);
        if (aggressionMod == 0) {
            return 1.0;
        }
        var self = twContext.getCurrentUnit();
        var distanceToClosestEnemy = twContext.getDistanceToClosestEnemy(self);

        // If there are no enemies at all, then this is as good as any other path
        if (distanceToClosestEnemy.isEmpty()) {
            return 1.0;
        }

        int distToEnemy = distanceToClosestEnemy.getAsInt();

        if ((distToEnemy == 0) && !(self instanceof Infantry)) {
            distToEnemy = 2;
        }

        double aggressionFraction = twContext.getBehaviorSettings().getHyperAggressionIndex() /  10.0;
        double closeness = 1.0 / (1.0 + distToEnemy);
        aggressionMod *= aggressionFraction * closeness;
        return clamp01(aggressionMod);
    }

    /**
     * Caution is a measure of how much the unit wants to avoid falling down on the curb while walking.
     */
    private double caution(TWDecisionContext twContext) {
        var fallShameMod = getDoubleParameter(cautionWeight);
        // If the fall shame mod is 0, then we don't care about caution
        if (fallShameMod == 0) {
            return 1.0;
        }

        var pilotingSuccess = twContext.getMovePathSuccessProbability();
        double fallShameFraction = twContext.getBehaviorSettings().getFallShameIndex() / 10.0;
        fallShameMod *= fallShameFraction;
        return clamp01(pilotingSuccess / fallShameMod);
    }

    /**
     * Self-preservation only applies when moving somewhere, 1 means "OK" to move there.
     */
    private double selfPreservation(TWDecisionContext twContext) {
        var selfPreservationMod = getDoubleParameter(selfPreservationWeight);
        if (selfPreservationMod == 0) {
            return 1.0;
        }
        var selfPreservation = twContext.getBehaviorSettings().getSelfPreservationValue() / 10.0;
        var self = twContext.getCurrentUnit();
        var distance = twContext.getDistanceToDestination();
        if (distance > 0) {
            var risk = 1 - (twContext.getExpectedDamage() / (double) self.getTotalArmor());
            return risk * selfPreservation;
        }
        return 1.0;
    }

    @Override
    public MyUnitBotSettings copy() {
        var copy = new MyUnitBotSettings();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
