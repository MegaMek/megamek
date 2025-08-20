/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common;

import java.util.HashMap;
import java.util.Map;

import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;

/**
 * An enum for the various rules levels
 *
 * @author Neoancient
 */
public enum SimpleTechLevel {
    INTRO("Introductory"),
    STANDARD("Standard"),
    ADVANCED("Advanced"),
    EXPERIMENTAL("Experimental"),
    UNOFFICIAL("Unofficial");

    private final String strVal;

    SimpleTechLevel(String strVal) {
        this.strVal = strVal;
    }

    public static SimpleTechLevel parse(String strVal) {
        for (SimpleTechLevel lvl : values()) {
            if (strVal.equals(lvl.strVal)) {
                return lvl;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return strVal;
    }

    /**
     * @return The more advanced of two tech levels.
     */
    public static SimpleTechLevel max(SimpleTechLevel l1, SimpleTechLevel l2) {
        if (l1.compareTo(l2) < 0) {
            return l2;
        } else {
            return l1;
        }
    }

    /**
     * @return The less advanced of two tech levels.
     */
    public static SimpleTechLevel min(SimpleTechLevel l1, SimpleTechLevel l2) {
        if (l1.compareTo(l2) > 0) {
            return l2;
        } else {
            return l1;
        }
    }

    /**
     * @return The corresponding TechConstants.T_* value.
     */
    public int getCompoundTechLevel(boolean clan) {
        switch (this) {
            case INTRO:
                return TechConstants.T_INTRO_BOXSET;
            case STANDARD:
                return clan ? TechConstants.T_CLAN_TW : TechConstants.T_IS_TW_NON_BOX;
            case ADVANCED:
                return clan ? TechConstants.T_CLAN_ADVANCED : TechConstants.T_IS_ADVANCED;
            case EXPERIMENTAL:
                return clan ? TechConstants.T_CLAN_EXPERIMENTAL : TechConstants.T_IS_EXPERIMENTAL;
            case UNOFFICIAL:
                return clan ? TechConstants.T_CLAN_UNOFFICIAL : TechConstants.T_IS_UNOFFICIAL;
            default:
                return TechConstants.T_INTRO_BOXSET;
        }
    }

    /**
     * Finds simple tech level equivalent of compound tech base/rules level constant
     *
     * @param level A TechConstants tech level constant
     *
     * @return
     */
    public static SimpleTechLevel convertCompoundToSimple(int level) {
        switch (level) {
            case TechConstants.T_INTRO_BOXSET:
                return SimpleTechLevel.INTRO;
            case TechConstants.T_IS_TW_NON_BOX:
            case TechConstants.T_CLAN_TW:
            case TechConstants.T_IS_TW_ALL:
            case TechConstants.T_TW_ALL:
                return SimpleTechLevel.STANDARD;
            case TechConstants.T_IS_ADVANCED:
            case TechConstants.T_CLAN_ADVANCED:
                return SimpleTechLevel.ADVANCED;
            case TechConstants.T_IS_EXPERIMENTAL:
            case TechConstants.T_CLAN_EXPERIMENTAL:
                return SimpleTechLevel.EXPERIMENTAL;
            case TechConstants.T_IS_UNOFFICIAL:
            case TechConstants.T_CLAN_UNOFFICIAL:
                return SimpleTechLevel.UNOFFICIAL;
            default:
                return SimpleTechLevel.STANDARD;
        }
    }

    public static SimpleTechLevel getGameTechLevel(Game game) {
        return SimpleTechLevel.parse(game.getOptions().stringOption(OptionsConstants.ALLOWED_TECH_LEVEL));
    }

    public static Map<Integer, String> getAllSimpleTechLevelCodeName() {
        Map<Integer, String> result = new HashMap<>();

        for (SimpleTechLevel simpleTechLevel : SimpleTechLevel.values()) {
            result.put(simpleTechLevel.ordinal(), simpleTechLevel.strVal);
        }

        return result;
    }
}
