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
import megamek.common.Coords;

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine the formation cohesion
 */
@JsonTypeName("FormationCohesion")
public class FormationCohesion extends TWConsideration {

    public static final String idealDistanceParam = "Ideal distance between units";
    private static final Map<String, Class<?>> parameterTypes = Map.of(idealDistanceParam, Integer.class);
    private static final Map<String, ParameterTitleTooltip> parameterTooltips = Map.of(idealDistanceParam,
        new ParameterTitleTooltip("IdealDistanceParam"));

    public FormationCohesion() {
        parameters.put(idealDistanceParam, 5);
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
    public double score(DecisionContext context) {
        var self = context.getCurrentUnit();

        Coords clusterCenter = context.getWorld().getEntityClusterCentroid(self);
        double distanceFromCenter = clusterCenter.distance(context.getFinalPosition());
        distanceFromCenter = Math.abs(distanceFromCenter - getIntParameter(idealDistanceParam));
        // this keeps the value at 1 when in the max formation distance,
        // and reduces it when going too close or too far away.
        return clamp01(1.0 - distanceFromCenter / ((double) getIntParameter(idealDistanceParam)));
    }

    @Override
    public FormationCohesion copy() {
        var copy = new FormationCohesion();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
