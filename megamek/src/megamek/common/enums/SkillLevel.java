/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.enums;

import static megamek.codeUtilities.MathUtility.clamp;

import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import megamek.MegaMek;
import megamek.logging.MMLogger;

public enum SkillLevel {
    // region Enum Declarations
    NONE("SkillLevel.NONE.text", "SkillLevel.NONE.toolTipText", "SkillLevel.NONE.shortName", 0),
    ULTRA_GREEN("SkillLevel.ULTRA_GREEN.text",
          "SkillLevel.ULTRA_GREEN.toolTipText",
          "SkillLevel.ULTRA_GREEN.shortName",
          1),
    GREEN("SkillLevel.GREEN.text", "SkillLevel.GREEN.toolTipText", "SkillLevel.GREEN.shortName", 2),
    REGULAR("SkillLevel.REGULAR.text", "SkillLevel.REGULAR.toolTipText", "SkillLevel.REGULAR.shortName", 3),
    VETERAN("SkillLevel.VETERAN.text", "SkillLevel.VETERAN.toolTipText", "SkillLevel.VETERAN.shortName", 4),
    ELITE("SkillLevel.ELITE.text", "SkillLevel.ELITE.toolTipText", "SkillLevel.ELITE.shortName", 5),
    HEROIC("SkillLevel.HEROIC.text", "SkillLevel.HEROIC.toolTipText", "SkillLevel.HEROIC.shortName", 6),
    LEGENDARY("SkillLevel.LEGENDARY.text", "SkillLevel.LEGENDARY.toolTipText", "SkillLevel.LEGENDARY.shortName", 7);
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    private final String shortName;
    private final int experienceLevel;
    // endregion Variable Declarations

    // region Constructors
    SkillLevel(final String name, final String toolTipText, final String shortName, final int experienceLevel) {
        final ResourceBundle resources = ResourceBundle.getBundle("megamek.common.messages",
              MegaMek.getMMOptions().getLocale());
        this.name = resources.getString(name);
        this.shortName = resources.getString(shortName);
        this.toolTipText = resources.getString(toolTipText);
        this.experienceLevel = experienceLevel;
    }
    // endregion Constructors

    // region Getters
    public String getToolTipText() {
        return toolTipText;
    }

    public String getShortName() {
        return shortName;
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

    public boolean isGreaterThan(SkillLevel skillLevel) {
        return (!this.equals(skillLevel) && this.experienceLevel > skillLevel.experienceLevel);
    }

    public boolean equalsOrGreaterThan(SkillLevel skillLevel) {
        return (this.equals(skillLevel) || (this.isGreaterThan(skillLevel)));
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
                MMLogger.create(SkillLevel.class).error(
                      "Attempting to get illegal default skill values for NONE Skill Level. Returning { 8, 8 }");
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

    /**
     * Adjusts a {@link SkillLevel} by a given delta, clamping the result within valid bounds.
     *
     * <p>This method increases or decreases the current skill level by the specified {@code delta}, ensuring that
     * the resulting experience level remains between {@link #ULTRA_GREEN} and {@link #LEGENDARY}. It then returns the
     * corresponding {@link SkillLevel} for the resulting experience level.
     *
     * @param current the current {@link SkillLevel} to adjust
     * @param delta   the change in experience level (positive to increase, negative to decrease)
     *
     * @return the resulting {@link SkillLevel} after applying the delta and clamping within bounds
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static SkillLevel changeByDelta(final SkillLevel current, final int delta) {
        int newExperienceLevel = clamp(current.experienceLevel + delta,
              ULTRA_GREEN.getExperienceLevel(),
              LEGENDARY.getExperienceLevel());
        return parseFromInteger(newExperienceLevel);
    }

    // region File I/O
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
                case 4:
                    return HEROIC;
                case 5:
                    return LEGENDARY;
                default:
                    break;
            }
        } catch (Exception ignored) {

        }

        MMLogger.create(SkillLevel.class).error("Unable to parse {} into a SkillLevel. Returning REGULAR.", text);

        return REGULAR;
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
            default -> throw new IllegalStateException(
                  "Unexpected value in megamek/common/enums/SkillLevel.java: " + value);
        };
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
