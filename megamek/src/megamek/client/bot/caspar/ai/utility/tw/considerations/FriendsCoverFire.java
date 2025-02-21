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
import megamek.ai.utility.StructOfUnitArrays;
import megamek.common.Coords;

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine if the unit has enough cover from its allies
 */
@JsonTypeName("FriendlyPositioning")
public class FriendsCoverFire extends TWConsideration {

    public static final String coverNumberParam = "Minimal number of friendlies covering you";
    private static final Map<String, Class<?>> parameterTypes = Map.of(coverNumberParam, Integer.class);
    private static final Map<String, ParameterTitleTooltip> parameterTooltips = Map.of(
        coverNumberParam, new ParameterTitleTooltip("CoverNumberParam"));

    public FriendsCoverFire() {
        parameters.put(coverNumberParam, 1);
    }

    public FriendsCoverFire(String name) {
        super(name);
        parameters.put(coverNumberParam, 1);
    }

    public FriendsCoverFire(String name, Curve curve) {
        super(name, curve);
        parameters.put(coverNumberParam, 1);
    }

    public FriendsCoverFire(String name, Curve curve, Map<String, Object> parameters) {
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
        var originX = context.getFinalPosition().getX();
        var originY = context.getFinalPosition().getY();
        var allies = context.getStructOfAlliesArrays().toArray();
        int count = 0;
        for (int[] unit : allies) {
            var x = unit[StructOfUnitArrays.Index.X.ordinal()];
            var y = unit[StructOfUnitArrays.Index.Y.ordinal()];
            var dist = Coords.distance(originX, originY, x, y);
            if (unit[StructOfUnitArrays.Index.MAX_RANGE.ordinal()] >= dist) {
                count++;
            }
        }
        return clamp01(count / Math.max(1.0, getIntParameter(coverNumberParam)));
    }

    @Override
    public FriendsCoverFire copy() {
        var copy = new FriendsCoverFire();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
