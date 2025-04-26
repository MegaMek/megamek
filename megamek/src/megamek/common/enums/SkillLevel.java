/*
 * Copyright (c) 2020-2025 - The MegaMek Team. All Rights Reserved.
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

import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import megamek.MegaMek;
import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;

public enum SkillLevel {
    // region Enum Declarations
    NONE("SkillLevel.NONE.text", "SkillLevel.NONE.toolTipText", 0),
    ULTRA_GREEN("SkillLevel.ULTRA_GREEN.text", "SkillLevel.ULTRA_GREEN.toolTipText", 1),
    GREEN("SkillLevel.GREEN.text", "SkillLevel.GREEN.toolTipText", 2),
    REGULAR("SkillLevel.REGULAR.text", "SkillLevel.REGULAR.toolTipText", 3),
    VETERAN("SkillLevel.VETERAN.text", "SkillLevel.VETERAN.toolTipText", 4),
    ELITE("SkillLevel.ELITE.text", "SkillLevel.ELITE.toolTipText", 5),
    HEROIC("SkillLevel.HEROIC.text", "SkillLevel.HEROIC.toolTipText", 6),
    LEGENDARY("SkillLevel.LEGENDARY.text", "SkillLevel.LEGENDARY.toolTipText", 7);
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    private final int experienceLevel;
    // endregion Variable Declarations

    // region Constructors
    SkillLevel(final String name, final String toolTipText, final int experienceLevel) {
        final ResourceBundle resources = ResourceBundle.getBundle("megamek.common.messages",
              MegaMek.getMMOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
        this.experienceLevel = experienceLevel;
    }
    // endregion Constructors

    // region Getters
    public String getToolTipText() {
        return toolTipText;
    }

    /**
     * Retrieves the current experience level of this entity. Where None is {@code 0}, Ultra-Green is {@code 1}, Green
     * is {@code 2} and so forth.
     *
     * @return the experience level as an integer.
     */
    public int getExperienceLevel() {
        return experienceLevel;
    }
    // endregion Getters

    // region Boolean Comparisons
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
    // endregion Boolean Comparisons

    /**
     * @return the skill level adjusted so that 0 is the level for Ultra-Green
     */
    public int getAdjustedValue() {
        return experienceLevel - 1;
    }

    /**
     * This returns the default skill values by level. This should never return the value for NONE, as NONE means one
     * does not have the skill.
     *
     * @return the default skill array pairing
     */
    public int[] getDefaultSkillValues() {
        return switch (this) {
            case NONE -> {
                MMLogger.create(SkillLevel.class)
                      .error("Attempting to get illegal default skill values for NONE Skill Level. Returning { 8, 8 }");
                yield new int[] { 8, 8 };
            }
            case ULTRA_GREEN -> new int[] { 6, 7 };
            case GREEN -> new int[] { 5, 6 };
            case VETERAN -> new int[] { 3, 4 };
            case ELITE -> new int[] { 2, 3 };
            case HEROIC -> new int[] { 1, 2 };
            case LEGENDARY -> new int[] { 0, 1 };
            default -> new int[] { 4, 5 };
        };
    }

    /**
     * @return a list of all skill levels that can be selected for random generation
     */
    public static List<SkillLevel> getGeneratableValues() {
        return Stream.of(values()).filter(skillLevel -> !skillLevel.isNone()).collect(Collectors.toList());
    }

    // region File I/O
    public static SkillLevel parseFromString(final String text) {
        // From String
        try {
            return valueOf(text.toUpperCase());
        } catch (Exception ignored) {
        }

        // From Name
        try {
            for (SkillLevel skillLevel : values()) {
                if (skillLevel.name().equalsIgnoreCase(text)) {
                    return skillLevel;
                }
            }
        } catch (Exception ignored) {
        }

        // From ordinal
        return SkillLevel.values()[MathUtility.parseInt(text, REGULAR.ordinal())];
    }

    /**
     * Parses an integer value to a {@link SkillLevel} enumeration.
     *
     * @param value the integer value to parse
     *
     * @return the {@link SkillLevel} enum corresponding to the given integer value
     *
     * @throws IllegalStateException if the integer value does not match any {@link SkillLevel} enum value
     */
    public static SkillLevel parseFromInteger(final int value) {
        return switch (value) {
            case 0 -> NONE;
            case 1 -> ULTRA_GREEN;
            case 2 -> GREEN;
            case 3 -> REGULAR;
            case 4 -> VETERAN;
            case 5 -> ELITE;
            case 6 -> HEROIC;
            case 7 -> LEGENDARY;
            default ->
                  throw new IllegalStateException("Unexpected value in megamek/common/enums/SkillLevel.java: " + value);
        };
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
