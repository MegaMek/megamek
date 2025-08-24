/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Mek;

/**
 * Contains some constants representing equipment/unit tech levels
 *
 * @author Ben
 * @since June 11, 2002, 4:35 PM
 */
public class TechConstants {

    /*
     * These can apply to entities or individual pieces of equipment.  These
     * values incorporate a tech level as well as a tech base.
     */
    public static final int T_ALLOWED_ALL = -2;
    public static final int T_TECH_UNKNOWN = -1;
    public static final int T_INTRO_BOX_SET = 0;
    public static final int T_IS_TW_NON_BOX = 1;
    public static final int T_CLAN_TW = 2;

    public static final int T_IS_TW_ALL = 3;
    public static final int T_TW_ALL = 4;

    public static final int T_IS_ADVANCED = 5;
    public static final int T_CLAN_ADVANCED = 6;

    public static final int T_IS_EXPERIMENTAL = 7;
    public static final int T_CLAN_EXPERIMENTAL = 8;

    public static final int T_IS_UNOFFICIAL = 9;
    public static final int T_CLAN_UNOFFICIAL = 10;

    public static final int T_ALL_IS = 11;
    public static final int T_ALL_CLAN = 12;
    public static final int T_ALL = 13;

    // Number of legal level 2 choices
    public static final int SIZE_LEVEL_2 = 5;

    // It must match the index to the constant's value.
    private static final String[] T_NAMES = { "IS_Box_Set", "IS_TW_Non_Box", "Clan_TW", "IS_TW", "All_TW",
                                              "IS_Advanced", "Clan_Advanced", "IS_Experimental", "Clan_Experimental",
                                              "IS_Unofficial", "Clan_Unofficial", "All_IS", "All_Clan", "All" };

    public static final int SIZE = T_NAMES.length;

    /**
     * These simple versions don't incorporate tech base (clan/is), and just represent a rules level.
     */
    public static final int T_SIMPLE_INTRO = 0;
    public static final int T_SIMPLE_STANDARD = 1;
    public static final int T_SIMPLE_ADVANCED = 2;
    public static final int T_SIMPLE_EXPERIMENTAL = 3;
    public static final int T_SIMPLE_UNOFFICIAL = 4;

    public static final String[] T_SIMPLE_NAMES = { "Introductory", "Standard", "Advanced", "Experimental",
                                                    "Unofficial" };

    public static final int SIMPLE_SIZE = T_SIMPLE_NAMES.length;

    // This translates the integer above into a simple level number.
    // The "all" selections return -1, since they don't apply to
    // individual units.
    public static final String[] T_SIMPLE_LEVEL = { "1", "2", "2", "-1", "-1", "3", "3", "4", "4", "5", "5", "-1", "-1",
                                                    "-1" };

    public static String getLevelName(int level) {
        if (level == T_ALLOWED_ALL) {
            return "(allowed to all)";
        }
        if (level == T_TECH_UNKNOWN) {
            return "(unknown tech level)";
        }
        if ((level >= 0) && (level < SIZE)) {
            return T_NAMES[level];
        }
        throw new IllegalArgumentException("Unknown tech level");
    }

    /**
     * Get the displayable name for the given tech level.
     *
     */
    public static String getLevelDisplayableName(int level) {
        if (level == T_ALLOWED_ALL) {
            return Messages.getString("TechLevel.T_ALLOWED_ALL");
        }
        if (level == T_TECH_UNKNOWN) {
            return Messages.getString("TechLevel.T_TECH_UNKNOWN");
        }
        if ((level >= 0) && (level < SIZE)) {
            return Messages.getString("TechLevel." + T_NAMES[level]);
        }
        throw new IllegalArgumentException("Unknown tech level");
    }

    public static String getSimpleLevelName(int level) {
        if (level == T_ALLOWED_ALL) {
            return Messages.getString("TechLevel.T_ALLOWED_ALL");
        }
        if (level == T_TECH_UNKNOWN) {
            return Messages.getString("TechLevel.T_TECH_UNKNOWN");
        }
        if ((level >= 0) && (level < SIMPLE_SIZE)) {
            return T_SIMPLE_NAMES[level];
        }
        throw new IllegalArgumentException("Unknown tech level");
    }

    /**
     * Return the numeric value for a simple level name. This is necessary because the options get stored as Strings
     * instead of ints, but it's easier to compare ints.
     *
     */
    public static int getSimpleLevel(String simpleLevel) {
        for (int i = 0; i < SIMPLE_SIZE; i++) {
            if (T_SIMPLE_NAMES[i].equals(simpleLevel)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Return the numeric value for a simple level name. This is necessary because the options get stored as Strings
     * instead of ints, but it's easier to compare ints.
     *
     */
    public static int getTechLevel(String techLevel) {
        for (int i = 0; i < SIZE; i++) {
            if (T_NAMES[i].equals(techLevel)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * @param game The current {@link Game}
     *
     * @return the Game's tech level as an integer.
     */
    public static int getSimpleLevel(Game game) {
        return getSimpleLevel(game.getOptions().stringOption(OptionsConstants.ALLOWED_TECH_LEVEL));
    }

    /**
     * Given a simple tech level and a tech base, convert to the lvl+base format
     *
     */
    public static int convertFromSimpleLevel(int simpleTechLvl, boolean isClan) {
        int legalLevel;
        switch (simpleTechLvl) {
            case TechConstants.T_SIMPLE_STANDARD:
                if (isClan) {
                    legalLevel = TechConstants.T_CLAN_TW;
                } else {
                    legalLevel = TechConstants.T_IS_TW_NON_BOX;
                }
                break;
            case TechConstants.T_SIMPLE_ADVANCED:
                if (isClan) {
                    legalLevel = TechConstants.T_CLAN_ADVANCED;
                } else {
                    legalLevel = TechConstants.T_IS_ADVANCED;
                }
                break;
            case TechConstants.T_SIMPLE_EXPERIMENTAL:
                if (isClan) {
                    legalLevel = TechConstants.T_CLAN_EXPERIMENTAL;
                } else {
                    legalLevel = TechConstants.T_IS_EXPERIMENTAL;
                }
                break;
            case TechConstants.T_SIMPLE_UNOFFICIAL:
                if (isClan) {
                    legalLevel = TechConstants.T_CLAN_UNOFFICIAL;
                } else {
                    legalLevel = TechConstants.T_IS_UNOFFICIAL;
                }
                break;
            default:
                legalLevel = TechConstants.T_INTRO_BOX_SET;

        }
        return legalLevel;
    }

    /**
     * Use the game's simple tech level and a flag to return the tech level + tech type.
     *
     * @param game   The current {@link Game}
     * @param isClan if the tech base is clan
     *
     * @return Tech Level and Type magic number to use for the current {@link Game}
     */
    public static int getGameTechLevel(Game game, boolean isClan) {
        // Get the integer simple level based on the string game option
        int simpleTechLvl = getSimpleLevel(game);
        // Arrays.binarySearch could return -1 if string isn't found
        simpleTechLvl = Math.max(0, simpleTechLvl);
        // Convert to TL+tech type
        return TechConstants.convertFromSimpleLevel(simpleTechLvl, isClan);
    }

    public static int convertFromNormalToSimple(int techLevel) {
        if (techLevel == T_ALLOWED_ALL) {
            return T_ALLOWED_ALL;
        }
        if (techLevel == T_TECH_UNKNOWN) {
            return T_TECH_UNKNOWN;
        }
        return Integer.parseInt(T_SIMPLE_LEVEL[techLevel]) - 1;
    }

    /**
     * Returns true if the equipment is legal for a unit with the paired tech levels; Returns false if it is not.
     */
    public static boolean isLegal(int entityTechLevel, int equipmentTechLevel, boolean mixed) {
        return TechConstants.isLegal(entityTechLevel, equipmentTechLevel, false, mixed);
    }

    /**
     * Returns true if the equipment is legal for a unit with the paired tech levels; Returns false if it is not.
     */
    public static boolean isLegal(int entityTechLevel, int equipmentTechLevel, boolean ignoreUnknown, boolean mixed) {
        // If it's allowed to all, ALWAYS return true.
        if (equipmentTechLevel == T_ALLOWED_ALL || equipmentTechLevel == T_ALL) {
            return true;
        }

        // If it's unknown, we're not going to be able to check it one way or the
        // other, so...
        if (equipmentTechLevel == T_TECH_UNKNOWN) {
            return ignoreUnknown;
        }

        // If they match, we're all good.
        if (entityTechLevel == equipmentTechLevel) {
            return true;
        }

        // If the entity is experimental and mixed, allow all but unofficial
        // if it's advanced and mixed, allow all but unofficial and experimental
        // if it's unofficial and mixed, allow everything
        if (mixed) {
            if (((entityTechLevel == T_IS_EXPERIMENTAL) || (entityTechLevel == T_CLAN_EXPERIMENTAL)) &&
                  ((equipmentTechLevel != T_IS_UNOFFICIAL) && (equipmentTechLevel != T_CLAN_UNOFFICIAL))) {
                return true;
            }
            if (((entityTechLevel == T_IS_ADVANCED) || (entityTechLevel == T_CLAN_ADVANCED)) &&
                  ((equipmentTechLevel != T_IS_UNOFFICIAL) && (equipmentTechLevel != T_CLAN_UNOFFICIAL)) &&
                  ((equipmentTechLevel != T_IS_EXPERIMENTAL) && (equipmentTechLevel != T_CLAN_EXPERIMENTAL))) {
                return true;
            }
            if ((entityTechLevel == T_IS_UNOFFICIAL) || (entityTechLevel == T_CLAN_UNOFFICIAL)) {
                return true;
            }
        }

        // If none of the catch-alls above are true, we go to specific cases

        // If the equipment is allowed to all clan and the entity is clan...
        if ((equipmentTechLevel == T_ALL_IS) && !isClan(entityTechLevel)) {
            return true;
        }

        // IS box set can be in any IS
        if ((equipmentTechLevel == T_INTRO_BOX_SET) &&
              ((entityTechLevel == T_IS_TW_NON_BOX) ||
                    (entityTechLevel == T_IS_TW_ALL) ||
                    (entityTechLevel == T_TW_ALL) ||
                    (entityTechLevel == T_IS_ADVANCED) ||
                    (entityTechLevel == T_IS_EXPERIMENTAL) ||
                    (entityTechLevel == T_IS_UNOFFICIAL) ||
                    (entityTechLevel == T_ALL))) {
            return true;
        }

        // IS TW stuff can be in any IS unit
        if ((equipmentTechLevel == T_IS_TW_NON_BOX ||
              equipmentTechLevel == T_TW_ALL ||
              equipmentTechLevel == T_IS_TW_ALL) &&
              ((entityTechLevel == T_IS_TW_ALL) ||
                    (entityTechLevel == T_IS_TW_NON_BOX) ||
                    (entityTechLevel == T_TW_ALL) ||
                    (entityTechLevel == T_IS_ADVANCED) ||
                    (entityTechLevel == T_IS_EXPERIMENTAL) ||
                    (entityTechLevel == T_IS_UNOFFICIAL) ||
                    (entityTechLevel == T_ALL))) {
            return true;
        }
        // clan TW stuff can be in any clan
        if ((equipmentTechLevel == T_CLAN_TW || equipmentTechLevel == T_TW_ALL) &&
              ((entityTechLevel == T_CLAN_TW) ||
                    (entityTechLevel == T_CLAN_ADVANCED) ||
                    (entityTechLevel == T_CLAN_EXPERIMENTAL) ||
                    (entityTechLevel == T_CLAN_UNOFFICIAL) ||
                    (entityTechLevel == T_ALL))) {
            return true;
        }
        // IS advanced stuff can be in IS advanced or higher
        if ((equipmentTechLevel == T_IS_ADVANCED) &&
              ((entityTechLevel == T_IS_EXPERIMENTAL) ||
                    (entityTechLevel == T_IS_UNOFFICIAL) ||
                    (entityTechLevel == T_ALL))) {
            return true;
        }

        // If the equipment is allowed to all clan and the entity is clan...
        if ((equipmentTechLevel == T_ALL_CLAN) && isClan(entityTechLevel)) {
            return true;
        }

        // clan advanced stuff can be in clan advanced or higher
        if ((equipmentTechLevel == T_CLAN_ADVANCED) &&
              ((entityTechLevel == T_CLAN_EXPERIMENTAL) ||
                    (entityTechLevel == T_CLAN_UNOFFICIAL) ||
                    (entityTechLevel == T_ALL))) {
            return true;
        }
        // IS experimental stuff can be in IS unofficial or all (identical level
        // is caught above
        if ((equipmentTechLevel == T_IS_EXPERIMENTAL) &&
              ((entityTechLevel == T_IS_UNOFFICIAL) || (entityTechLevel == T_ALL))) {
            return true;
        }
        // clan experimental stuff can be in clan unofficial or all (identical
        // level
        // is caught above
        return (equipmentTechLevel == T_CLAN_EXPERIMENTAL) &&
              ((entityTechLevel == T_CLAN_UNOFFICIAL) || (entityTechLevel == T_ALL));
    }

    public static String getTechName(int level) {
        if ((level == T_INTRO_BOX_SET) ||
              (level == T_IS_TW_NON_BOX) ||
              (level == T_IS_ADVANCED) ||
              (level == T_IS_EXPERIMENTAL) ||
              (level == T_IS_UNOFFICIAL)) {
            return "Inner Sphere";
        } else if ((level == T_CLAN_TW) ||
              (level == T_CLAN_ADVANCED) ||
              (level == T_CLAN_EXPERIMENTAL) ||
              (level == T_CLAN_UNOFFICIAL)) {
            return "Clan";
        } else if (level == T_ALLOWED_ALL) {
            return "IS/Clan";
        } else {
            return "(Unknown Technology Base)";
        }
    }

    public static boolean isClan(int level) {
        return switch (level) {
            case T_CLAN_TW, T_CLAN_ADVANCED, T_CLAN_EXPERIMENTAL, T_CLAN_UNOFFICIAL, T_ALL_CLAN -> true;
            default -> false;
        };
    }

    public static int getOppositeTechLevel(int level) {
        return switch (level) {
            case T_INTRO_BOX_SET, T_IS_TW_NON_BOX -> T_CLAN_TW;
            case T_IS_ADVANCED -> T_CLAN_ADVANCED;
            case T_IS_EXPERIMENTAL -> T_CLAN_EXPERIMENTAL;
            case T_IS_UNOFFICIAL -> T_CLAN_UNOFFICIAL;
            case T_CLAN_TW -> T_IS_TW_NON_BOX;
            case T_CLAN_ADVANCED -> T_IS_ADVANCED;
            case T_CLAN_EXPERIMENTAL -> T_IS_EXPERIMENTAL;
            case T_CLAN_UNOFFICIAL -> T_IS_UNOFFICIAL;
            default -> T_TECH_UNKNOWN;
        };
    }

    /**
     * Return the tech level of the given gyro. This is necessary because gyros are systems and hence don't have
     * MiscType entries.
     *
     */
    public static int getGyroTechLevel(int gyroType, boolean isClan, int year) {
        switch (gyroType) {
            case Mek.GYRO_STANDARD:
                if (isClan) {
                    if (year <= 2807) {
                        return T_CLAN_UNOFFICIAL;
                    }
                    return T_CLAN_TW;
                }

                if (year <= 2295) {
                    return T_IS_UNOFFICIAL;
                } else if (year <= 2350) {
                    return T_IS_EXPERIMENTAL;
                } else if (year < 2505) {
                    return T_IS_ADVANCED;
                } else {
                    return T_INTRO_BOX_SET;
                }
            case Mek.GYRO_XL, Mek.GYRO_HEAVY_DUTY:
                if (isClan) {
                    return T_CLAN_UNOFFICIAL;
                }
                if (year <= 3050) {
                    return T_IS_UNOFFICIAL;
                } else if (year <= 3067) {
                    return T_IS_EXPERIMENTAL;
                } else if (year <= 3072) {
                    return T_IS_ADVANCED;
                } else {
                    return T_IS_TW_NON_BOX;
                }

            case Mek.GYRO_COMPACT:
                if (isClan) {
                    return T_CLAN_UNOFFICIAL;
                }
                if (year <= 3050) {
                    return T_IS_UNOFFICIAL;
                } else if (year <= 3068) {
                    return T_IS_EXPERIMENTAL;
                } else if (year <= 3072) {
                    return T_IS_ADVANCED;
                } else {
                    return T_IS_TW_NON_BOX;
                }
            case Mek.GYRO_SUPERHEAVY:
                if (isClan) {
                    return T_CLAN_UNOFFICIAL;
                }
                if (year <= 2900) {
                    return T_IS_UNOFFICIAL;
                } else if (year <= 2940) {
                    return T_IS_EXPERIMENTAL;
                } else {
                    return T_IS_ADVANCED;
                }
            case Mek.GYRO_NONE:
                return T_ALLOWED_ALL;
        }

        return T_TECH_UNKNOWN;
    }

    /**
     * Return the tech level of the given cockpit. THis is necessary because cockpits are systems and hence don't have
     * MiscType entries.
     *
     */
    public static int getCockpitTechLevel(int cockpitType, long entityType, boolean isClan, int year) {
        if ((entityType & Entity.ETYPE_MEK) != 0) {
            switch (cockpitType) {
                case Mek.COCKPIT_STANDARD:
                    if (isClan) {
                        if (year <= 2807) {
                            return T_CLAN_UNOFFICIAL;
                        }
                        return T_CLAN_TW;
                    }
                    if (year <= 2463) {
                        return T_IS_UNOFFICIAL;
                    } else if (year <= 2470) {
                        return T_IS_EXPERIMENTAL;
                    } else if (year <= 2487) {
                        return T_IS_ADVANCED;
                    } else {
                        return T_INTRO_BOX_SET;
                    }
                case Mek.COCKPIT_SMALL:
                    if (isClan) {
                        if (year <= 3080) {
                            return T_CLAN_UNOFFICIAL;
                        }
                        return T_CLAN_ADVANCED;
                    }
                    if (year <= 3060) {
                        return T_IS_UNOFFICIAL;
                    } else if (year <= 3067) {
                        return T_IS_EXPERIMENTAL;
                    } else if (year <= 3080) {
                        return T_IS_ADVANCED;
                    } else {
                        return T_IS_TW_NON_BOX;
                    }
                case Mek.COCKPIT_PRIMITIVE:
                    if (isClan) {
                        if (year <= 2807) {
                            return T_CLAN_UNOFFICIAL;
                        }
                        return T_CLAN_ADVANCED;
                    }

                    if (year <= 2425) {
                        return T_IS_UNOFFICIAL;
                    } else if (year <= 2439) {
                        return T_IS_EXPERIMENTAL;
                    } else if (year <= 2520) {
                        return T_IS_ADVANCED;
                    }
                case Mek.COCKPIT_PRIMITIVE_INDUSTRIAL:
                    if (isClan) {
                        return T_CLAN_UNOFFICIAL;
                    }
                    if (year <= 2295) {
                        return T_IS_UNOFFICIAL;
                    } else if (year <= 2350) {
                        return T_IS_EXPERIMENTAL;
                    } else if (year <= 2520) {
                        return T_IS_ADVANCED;
                    }
                case Mek.COCKPIT_INDUSTRIAL:
                    // Not sure how to handle the Adv. Fire Control One.
                    // With advanced Fire Control becomes Non-Box in 2491
                    if (isClan) {
                        if (year <= 2807) {
                            return T_CLAN_UNOFFICIAL;
                        }
                        return T_CLAN_TW;
                    }
                    if (year <= 2464) {
                        return T_IS_UNOFFICIAL;
                    } else if (year <= 2470) {
                        return T_IS_EXPERIMENTAL;
                    } else if (year <= 2490) {
                        return T_IS_ADVANCED;
                    } else {
                        return T_IS_TW_NON_BOX;
                    }
                case Mek.COCKPIT_TORSO_MOUNTED:
                    if (isClan) {
                        if (year <= 3055) {
                            return T_CLAN_UNOFFICIAL;
                        } else if (year < 3070) {
                            return T_CLAN_ADVANCED;
                        } else {
                            return T_CLAN_TW;
                        }
                    } else {
                        if (year <= 3053) {
                            return T_IS_UNOFFICIAL;
                        } else if (year <= 3070) {
                            return T_IS_ADVANCED;
                        } else {
                            return T_IS_TW_NON_BOX;
                        }
                    }
                case Mek.COCKPIT_INTERFACE: // Clan Version
                    if (isClan) {
                        if (year <= 3078) {
                            return T_CLAN_UNOFFICIAL;
                        }
                        return T_CLAN_EXPERIMENTAL;
                    }
                    if (year <= 3069) {
                        return T_IS_UNOFFICIAL;
                    } else {
                        return T_IS_EXPERIMENTAL;
                    }
                case Mek.COCKPIT_COMMAND_CONSOLE:
                    if (isClan) {
                        if (year <= 2807) {
                            return T_CLAN_UNOFFICIAL;
                        }
                        return T_CLAN_ADVANCED;
                    }
                    if (year <= 2620) {
                        return T_IS_UNOFFICIAL;
                    } else if (year <= 2631) {
                        return T_IS_EXPERIMENTAL;
                    } else if (year <= 2845) {
                        return T_IS_ADVANCED;
                    } else if (year <= 3025) {
                        return T_IS_UNOFFICIAL;
                    } else {
                        return T_IS_ADVANCED;
                    }
                case Mek.COCKPIT_TRIPOD:
                    if (isClan) {
                        return T_CLAN_UNOFFICIAL;
                    }

                    if (year <= 2585) {
                        return T_IS_UNOFFICIAL;
                    } else if (year <= 2602) {
                        return T_IS_EXPERIMENTAL;
                    } else {
                        return T_IS_ADVANCED;
                    }

                case Mek.COCKPIT_SUPERHEAVY:
                    if (isClan) {
                        return T_CLAN_UNOFFICIAL;
                    }

                    if (year <= 3055) {
                        return T_IS_UNOFFICIAL;
                    } else if (year <= 3076) {
                        return T_IS_EXPERIMENTAL;
                    } else {
                        return T_IS_ADVANCED;
                    }
                case Mek.COCKPIT_SUPERHEAVY_TRIPOD:
                    if (isClan) {
                        return T_CLAN_UNOFFICIAL;
                    }

                    if (year <= 3125) {
                        return T_IS_UNOFFICIAL;
                    } else if (year <= 3135) {
                        return T_IS_EXPERIMENTAL;
                    } else {
                        return T_IS_ADVANCED;
                    }
                case Mek.COCKPIT_VRRP:
                    if (isClan) {
                        return T_CLAN_UNOFFICIAL;
                    }

                    if (year <= 3052) {
                        return T_IS_UNOFFICIAL;
                    } else if (year <= 3055) {
                        return T_IS_EXPERIMENTAL;
                    } else {
                        return T_IS_UNOFFICIAL;
                    }
                case Mek.COCKPIT_QUADVEE:
                    if (isClan) {
                        if (year <= 3125) {
                            return T_CLAN_UNOFFICIAL;
                        } else if (year <= 3135) {
                            return T_CLAN_EXPERIMENTAL;
                        } else {
                            return T_CLAN_ADVANCED;
                        }
                    }
                    return T_IS_UNOFFICIAL;
                case Mek.COCKPIT_SUPERHEAVY_INDUSTRIAL:
                    if (isClan) {
                        return T_CLAN_UNOFFICIAL;
                    }

                    if (year <= 2900) {
                        return T_IS_UNOFFICIAL;
                    } else if (year <= 2940) {
                        return T_IS_EXPERIMENTAL;
                    } else {
                        return T_IS_ADVANCED;
                    }
                    //Same as Superheavy cockpit
                case Mek.COCKPIT_SUPERHEAVY_COMMAND_CONSOLE:
                    if (isClan) {
                        return T_CLAN_UNOFFICIAL;
                    }

                    if (year <= 3055) {
                        return T_IS_UNOFFICIAL;
                    } else if (year <= 3076) {
                        return T_IS_EXPERIMENTAL;
                    } else {
                        return T_IS_ADVANCED;
                    }
                    //Same as Small cockpit
                case Mek.COCKPIT_SMALL_COMMAND_CONSOLE:
                    if (isClan) {
                        if (year <= 3080) {
                            return T_CLAN_UNOFFICIAL;
                        }
                        return T_CLAN_ADVANCED;
                    }
                    if (year <= 3060) {
                        return T_IS_UNOFFICIAL;
                    } else if (year <= 3067) {
                        return T_IS_EXPERIMENTAL;
                    } else if (year <= 3080) {
                        return T_IS_ADVANCED;
                    } else {
                        return T_IS_TW_NON_BOX;
                    }

            }
        } else if ((entityType & Entity.ETYPE_AERO) != 0) {
            switch (cockpitType) {
                case Aero.COCKPIT_PRIMITIVE:
                    if (isClan) {
                        return T_CLAN_ADVANCED;
                    }

                    if (year <= 2100) {
                        return T_IS_UNOFFICIAL;
                    } else if (year <= 2295) {
                        return T_IS_EXPERIMENTAL;
                    } else if (year <= 2520) {
                        return T_IS_ADVANCED;
                    }
                case Aero.COCKPIT_STANDARD:
                    if (isClan) {
                        if (year <= 2807) {
                            return T_CLAN_UNOFFICIAL;
                        }
                        return T_CLAN_TW;
                    }
                    if (year <= 2455) {
                        return T_IS_UNOFFICIAL;
                    } else if (year <= 2470) {
                        return T_IS_EXPERIMENTAL;
                    } else if (year <= 2491) {
                        return T_IS_ADVANCED;
                    } else {
                        return T_INTRO_BOX_SET;
                    }
                case Aero.COCKPIT_SMALL:
                    if (isClan) {
                        if (year <= 3080) {
                            return T_CLAN_UNOFFICIAL;
                        }
                        return T_CLAN_ADVANCED;
                    }
                    if (year <= 3060) {
                        return T_IS_UNOFFICIAL;
                    } else if (year <= 3070) {
                        return T_IS_EXPERIMENTAL;
                    } else if (year <= 3080) {
                        return T_IS_ADVANCED;
                    } else {
                        return T_IS_TW_NON_BOX;
                    }
                case Aero.COCKPIT_COMMAND_CONSOLE:
                    if (isClan) {
                        if (year <= 2807) {
                            return T_CLAN_UNOFFICIAL;
                        }
                        return T_CLAN_TW;
                    }
                    if (year <= 2620) {
                        return T_IS_UNOFFICIAL;
                    } else if (year <= 2631) {
                        return T_IS_EXPERIMENTAL;
                    } else if (year <= 2855) {
                        return T_IS_ADVANCED;
                    } else if (year <= 3025) {
                        return T_IS_UNOFFICIAL;
                    } else {
                        return T_IS_ADVANCED;
                    }
            }
        }
        return T_TECH_UNKNOWN;
    }
}
