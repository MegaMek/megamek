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

import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * This consideration is used to determine friendly artillery fire risk
 */
@JsonTypeName("DecoyValue")
public class DecoyValue extends TWConsideration {

    public static final String minJumpParam = "Minimal jump MP";
    public static final String minRunParam = "Minimal run MP";

    private static final Map<String, Class<?>> parameterTypes = Map.of(
        minJumpParam, Integer.class,
        minRunParam, Integer.class);
    private static final Map<String, ParameterTitleTooltip> parameterTooltips = Map.of(
        minJumpParam, new ParameterTitleTooltip("MinJumpParam"),
        minRunParam, new ParameterTitleTooltip("MinRunParam")
    );

    @Override
    public Map<String, Class<?>> getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public Map<String, ParameterTitleTooltip> getParameterTooltips() {
        return parameterTooltips;
    }

    public DecoyValue() {
        this.parameters.put(minJumpParam, 7);
        this.parameters.put(minRunParam, 12);
    }

    @Override
    public double score(DecisionContext context) {
        var currentUnit = context.getCurrentUnit();

        if ((currentUnit.getJumpMP() >= getIntParameter(minJumpParam))
            || (currentUnit.getRunMP() >= getIntParameter(minRunParam))) {
            return clamp01(1 - (currentUnit.getDamageLevel() / 4.0));
        }

        return currentUnit.getDamageLevel() / 8.0;
    }

    @Override
    public DecoyValue copy() {
        var copy = new DecoyValue();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}
