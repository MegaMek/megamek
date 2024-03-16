/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.enums;

import megamek.MegaMek;
import org.apache.logging.log4j.LogManager;

import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum SkillLevel {
    //region Enum Declarations
    NONE("SkillLevel.NONE.text", "SkillLevel.NONE.toolTipText"),
    ULTRA_GREEN("SkillLevel.ULTRA_GREEN.text", "SkillLevel.ULTRA_GREEN.toolTipText"),
    GREEN("SkillLevel.GREEN.text", "SkillLevel.GREEN.toolTipText"),
    REGULAR("SkillLevel.REGULAR.text", "SkillLevel.REGULAR.toolTipText"),
    VETERAN("SkillLevel.VETERAN.text", "SkillLevel.VETERAN.toolTipText"),
    ELITE("SkillLevel.ELITE.text", "SkillLevel.ELITE.toolTipText"),
    HEROIC("SkillLevel.HEROIC.text", "SkillLevel.HEROIC.toolTipText"),
    LEGENDARY("SkillLevel.LEGENDARY.text", "SkillLevel.LEGENDARY.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    SkillLevel(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("megamek.common.messages",
                MegaMek.getMMOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

    //region Boolean Comparisons
    public boolean isNone() {
        return this == NONE;
    }

    public boolean isUltraGreen() {
        return this == ULTRA_GREEN;
    }

    public boolean isGreen() {
        return this == GREEN;
    }

    public boolean isRegular() {
        return this == REGULAR;
    }

    public boolean isVeteran() {
        return this == VETERAN;
    }

    public boolean isElite() {
        return this == ELITE;
    }

    public boolean isHeroic() {
        return this == HEROIC;
    }

    public boolean isLegendary() {
        return this == LEGENDARY;
    }

    public boolean isUltraGreenOrGreater() {
        return isUltraGreen() || isGreenOrGreater();
    }

    public boolean isGreenOrGreater() {
        return isGreen() || isRegularOrGreater();
    }

    public boolean isRegularOrGreater() {
        return isRegular() || isVeteranOrGreater();
    }

    public boolean isVeteranOrGreater() {
        return isVeteran() || isEliteOrGreater();
    }

    public boolean isEliteOrGreater() {
        return isElite() || isHeroicOrGreater();
    }

    public boolean isHeroicOrGreater() {
        return isHeroic() || isLegendary();
    }
    //endregion Boolean Comparisons

    /**
     * @return the skill level adjusted so that 0 is the level for Ultra-Green
     */
    public int getAdjustedValue() {
        return ordinal() - 1;
    }

    /**
     * This returns the default skill values by level. This should never return the value for NONE,
     * as NONE means one does not have the skill.
     * @return the default skill array pairing
     */
    public int[] getDefaultSkillValues() {
        switch (this) {
            case NONE:
                LogManager.getLogger().error("Attempting to get illegal default skill values for NONE Skill Level. Returning { 8, 8 }");
                return new int[] { 8, 8 };
            case ULTRA_GREEN:
                return new int[] { 6, 7 };
            case GREEN:
                return new int[] { 5, 6 };
            case VETERAN:
                return new int[] { 3, 4 };
            case ELITE:
                return new int[] { 2, 3 };
            case HEROIC:
                return new int[] { 1, 2 };
            case LEGENDARY:
                return new int[] { 0, 1 };
            case REGULAR:
            default:
                return new int[] { 4, 5 };
        }
    }

    /**
     * @return a list of all skill levels that can be selected for random generation
     */
    public static List<SkillLevel> getGeneratableValues() {
        return Stream.of(values()).filter(skillLevel -> !skillLevel.isNone()).collect(Collectors.toList());
    }

    //region File I/O
    public static SkillLevel parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 0:
                    return GREEN;
                case 1:
                    return REGULAR;
                case 2:
                    return VETERAN;
                case 3:
                    return ELITE;
                default:
                    break;
            }
        } catch (Exception ignored) {

        }

        LogManager.getLogger().error("Unable to parse " + text + " into a SkillLevel. Returning REGULAR.");

        return REGULAR;
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
