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
import megamek.ai.utility.Curve;
import megamek.ai.utility.DecisionContext;
import megamek.ai.utility.ParameterTitleTooltip;
import megamek.common.Coords;
import megamek.common.GunEmplacement;
import megamek.common.Mek;
import megamek.common.Tank;

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine
 */
@JsonTypeName("CrowdingFriends")
public class CrowdingFriends extends TWConsideration {

    public static final String herdingDistanceParam = "crowding distance tolerance";
    private static final Map<String, Class<?>> parameterTypes = Map.of(herdingDistanceParam, Integer.class);
    private static final Map<String, ParameterTitleTooltip> parameterTooltips = Map.of(
        herdingDistanceParam, new ParameterTitleTooltip("HerdingDistance"));

    public CrowdingFriends() {
        parameters.put(herdingDistanceParam, 6);
    }

    public CrowdingFriends(String name) {
        super(name);
        parameters.put(herdingDistanceParam, 6);
    }

    public CrowdingFriends(String name, Curve curve) {
        super(name, curve);
        parameters.put(herdingDistanceParam, 6);
    }

    public CrowdingFriends(String name, Curve curve, Map<String, Object> parameters) {
        super(name, curve, parameters);
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
        Coords position = context.getFinalPosition();
        if (!(self instanceof Mek) && !(self instanceof Tank) || (self instanceof GunEmplacement)) {
            return 1.0;
        }

        int herdingDistance = getIntParameter(herdingDistanceParam);
        int crowdingFriends = context.getFriendliesWithinRange(position, herdingDistance).size();
        double divider = 6 * herdingDistance + 1.0;
        return clamp01(crowdingFriends / divider);
    }

    @Override
    public CrowdingFriends copy() {
        var copy = new CrowdingFriends();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
