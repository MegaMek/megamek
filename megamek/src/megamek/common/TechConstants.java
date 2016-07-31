/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

/*
 * TechConstants.java
 *
 * Created on June 11, 2002, 4:35 PM
 */

package megamek.common;

import java.util.Arrays;

/**
 * Contains some constants representing equipment/unit tech levels
 *
 * @author Ben
 * @version
 */
public class TechConstants {

    /*
     * These can apply to entities or individual pieces of equipment.  These
     * values incorporate a tech level as well as a tech base.
     */
    public static final int T_ALLOWED_ALL = -2;
    public static final int T_TECH_UNKNOWN = -1;
    public static final int T_INTRO_BOXSET = 0;
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

    // this made public because MekWars accesses it
    // It must match the index to the constant's value.
    public static final String[] T_NAMES = { "IS_Box_Set", "IS_TW_Non_Box",
            "Clan_TW", "IS_TW", "All_TW", "IS_Advanced", "Clan_Advanced",
            "IS_Experimental", "Clan_Experimental", "IS_Unofficial",
            "Clan_Unofficial", "All_IS", "All_Clan", "All" };

    public static final int SIZE = T_NAMES.length;

    /**
     * These simple versions don't incorporate tech base (clan/is), and just
     * represent a rules level.
     */
    public static final int T_SIMPLE_INTRO = 0;
    public static final int T_SIMPLE_STANDARD = 1;
    public static final int T_SIMPLE_ADVANCED = 2;
    public static final int T_SIMPLE_EXPERIMENTAL = 3;
    public static final int T_SIMPLE_UNOFFICIAL = 4;

    public static final String[] T_SIMPLE_NAMES = { "Introductory", "Standard",
            "Advanced", "Experimental", "Unofficial" };

    public static final int SIMPLE_SIZE = T_SIMPLE_NAMES.length;

    // This translates the integer above into a simple level number.
    // The "all" selections return -1, since they don't apply to
    // individual units.
    public static final String[] T_SIMPLE_LEVEL = { "1", "2", "2", "-1", "-1",
            "3", "3", "4", "4", "5", "5", "-1", "-1", "-1" };

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
     * @param level
     * @return
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
     * Return the numeric value for a simple level name. This is necessary
     * because the options get stored as Strings instead of ints, but it's
     * easier to compare ints.
     *
     * @param simpleLevel
     * @return
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
     * Return the numeric value for a simple level name. This is necessary
     * because the options get stored as Strings instead of ints, but it's
     * easier to compare ints.
     *
     * @param simpleLevel
     * @return
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
     * Returns the Game's tech level as an integer.
     *
     * @param game
     * @return
     */
    public static int getSimpleLevel(IGame game) {
        return getSimpleLevel(game.getOptions().stringOption("techlevel"));
    }

    /**
     * Given a simple tech level and a tech base, convert to the lvl+base format
     *
     * @param simpleTechLvl
     * @param isClan
     * @return
     */
    public static int convertFromSimplelevel(int simpleTechLvl, boolean isClan) {
        int legalLevel;
        switch (simpleTechLvl) {
            case TechConstants.T_SIMPLE_INTRO:
                legalLevel = TechConstants.T_INTRO_BOXSET;
                break;
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
                legalLevel = TechConstants.T_INTRO_BOXSET;

        }
        return legalLevel;
    }

    /**
     * Use the game's simple tech level and a flag to return the tech level +
     * tech type.
     *
     * @param game
     * @param isClan
     * @return
     */
    public static int getGameTechLevel(IGame game, boolean isClan) {
        // Get the integer simple level based on the string game option
        int simpleTechLvl = Arrays.binarySearch(TechConstants.T_SIMPLE_NAMES, game
                .getOptions().stringOption("techlevel")); //$NON-NLS-1$
        // Arrays.binarySearch could return -1 if string isn't found
        simpleTechLvl = Math.max(0, simpleTechLvl);
        // Convert to TL+tech type
        return TechConstants.convertFromSimplelevel(simpleTechLvl, isClan);
    }

    public static int convertFromNormalToSimple(int techLevel) {
        if (techLevel == T_ALLOWED_ALL) {
            return T_ALLOWED_ALL;
        }
        if (techLevel == T_TECH_UNKNOWN) {
            return T_TECH_UNKNOWN;
        }
        int simpleTL = Integer.parseInt(T_SIMPLE_LEVEL[techLevel]) - 1;
        return simpleTL;
    }

    /**
     * Returns true if the equipment is legal for a unit with the paired tech
     * levels; Returns false if it is not.
     */
    public static boolean isLegal(int entityTechlevel, int equipmentTechlevel,
            boolean mixed) {
        return TechConstants.isLegal(entityTechlevel, equipmentTechlevel,
                false, mixed);
    }

    /**
     * Returns true if the equipment is legal for a unit with the paired tech
     * levels; Returns false if it is not.
     */
    public static boolean isLegal(int entityTechlevel, int equipmentTechlevel,
            boolean ignoreUnknown, boolean mixed) {
        // If it's allowed to all, ALWAYS return true.
        if (equipmentTechlevel == T_ALLOWED_ALL) {
            return true;
        }

        // If it's unknown, we're not gonna be able to check it one way or the
        // other, so...
        if (equipmentTechlevel == T_TECH_UNKNOWN) {
            if (ignoreUnknown) {
                return true;
            }
            return false;
        }

        // If they match, we're all good.
        if (entityTechlevel == equipmentTechlevel) {
            return true;
        }

        // If the entity is experimental and mixed, allow all but unofficial
        // if it's advanced and mixed, allow all but unofficial and experimental
        // if it's unofficial and mixed, allow everything
        if (mixed) {
            if (((entityTechlevel == T_IS_EXPERIMENTAL) || (entityTechlevel == T_CLAN_EXPERIMENTAL))
                    && ((equipmentTechlevel != T_IS_UNOFFICIAL) && (equipmentTechlevel != T_CLAN_UNOFFICIAL))) {
                return true;
            }
            if (((entityTechlevel == T_IS_ADVANCED) || (entityTechlevel == T_CLAN_ADVANCED))
                    && ((equipmentTechlevel != T_IS_UNOFFICIAL) && (equipmentTechlevel != T_CLAN_UNOFFICIAL))
                    && ((equipmentTechlevel != T_IS_EXPERIMENTAL) && (equipmentTechlevel != T_CLAN_EXPERIMENTAL))) {
                return true;
            }
            if ((entityTechlevel == T_IS_UNOFFICIAL)
                    || (entityTechlevel == T_CLAN_UNOFFICIAL)) {
                return true;
            }
        }

        // If none of the catch-alls above are true, we go to specific cases

        // If the equipment is allowed to all clan and the entity is clan...
        if ((equipmentTechlevel == T_ALL_IS) && !isClan(entityTechlevel)){
            return true;
        }
        
        // IS box set can be in any IS
        if ((equipmentTechlevel == T_INTRO_BOXSET)
                && ((entityTechlevel == T_IS_TW_NON_BOX)
                        || (entityTechlevel == T_IS_TW_ALL)
                        || (entityTechlevel == T_TW_ALL)
                        || (entityTechlevel == T_IS_ADVANCED)
                        || (entityTechlevel == T_IS_EXPERIMENTAL)
                        || (entityTechlevel == T_IS_UNOFFICIAL) || (entityTechlevel == T_ALL))) {
            return true;
        }

        // IS TW stuff can be in any IS unit
        if ((equipmentTechlevel == T_IS_TW_NON_BOX)
                && ((entityTechlevel == T_IS_TW_ALL)
                        || (entityTechlevel == T_TW_ALL)
                        || (entityTechlevel == T_IS_ADVANCED)
                        || (entityTechlevel == T_IS_EXPERIMENTAL)
                        || (entityTechlevel == T_IS_UNOFFICIAL) || (entityTechlevel == T_ALL))) {
            return true;
        }
        // clan TW stuff can be in any clan
        if ((equipmentTechlevel == T_CLAN_TW)
                && ((entityTechlevel == T_CLAN_TW)
                        || (entityTechlevel == T_CLAN_ADVANCED)
                        || (entityTechlevel == T_CLAN_EXPERIMENTAL)
                        || (entityTechlevel == T_CLAN_UNOFFICIAL) || (entityTechlevel == T_ALL))) {
            return true;
        }
        // IS advanced stuff can be in IS advanced or higher
        if ((equipmentTechlevel == T_IS_ADVANCED)
                && ((entityTechlevel == T_IS_EXPERIMENTAL)
                        || (entityTechlevel == T_IS_UNOFFICIAL) || (entityTechlevel == T_ALL))) {
            return true;
        }
        
        // If the equipment is allowed to all clan and the entity is clan...
        if ((equipmentTechlevel == T_ALL_CLAN) && isClan(entityTechlevel)){
            return true;
        }
        
        // clan advanced stuff can be in clan advanced or higher
        if ((equipmentTechlevel == T_CLAN_ADVANCED)
                && ((entityTechlevel == T_CLAN_EXPERIMENTAL)
                        || (entityTechlevel == T_CLAN_UNOFFICIAL) || (entityTechlevel == T_ALL))) {
            return true;
        }
        // IS experimental stuff can be in IS unoffical or all (identical level
        // is caught above
        if ((equipmentTechlevel == T_IS_EXPERIMENTAL)
                && ((entityTechlevel == T_IS_UNOFFICIAL) || (entityTechlevel == T_ALL))) {
            return true;
        }
        // clan experimental stuff can be in clan unoffical or all (identical
        // level
        // is caught above
        if ((equipmentTechlevel == T_CLAN_EXPERIMENTAL)
                && ((entityTechlevel == T_CLAN_UNOFFICIAL) || (entityTechlevel == T_ALL))) {
            return true;
        }
        return false;
    }

    public static String getTechName(int level) {
        if ((level == T_INTRO_BOXSET) || (level == T_IS_TW_NON_BOX)
                || (level == T_IS_ADVANCED) || (level == T_IS_EXPERIMENTAL)
                || (level == T_IS_UNOFFICIAL)) {
            return "Inner Sphere";
        } else if ((level == T_CLAN_TW) || (level == T_CLAN_ADVANCED)
                || (level == T_CLAN_EXPERIMENTAL)
                || (level == T_CLAN_UNOFFICIAL)) {
            return "Clan";
        } else if (level == T_ALLOWED_ALL) {
            return "IS/Clan";
        } else {
            return "(Unknown Technology Base)";
        }
    }

    public static boolean isClan(int level) {
        switch (level) {
            case T_CLAN_TW:
            case T_CLAN_ADVANCED:
            case T_CLAN_EXPERIMENTAL:
            case T_CLAN_UNOFFICIAL:
            case T_ALL_CLAN:
                return true;
            default:
                return false;
        }
    }

    public static int getOppositeTechLevel(int level) {
        switch (level) {
            case T_INTRO_BOXSET:
            case T_IS_TW_NON_BOX:
                return T_CLAN_TW;
            case T_IS_ADVANCED:
                return T_CLAN_ADVANCED;
            case T_IS_EXPERIMENTAL:
                return T_CLAN_EXPERIMENTAL;
            case T_IS_UNOFFICIAL:
                return T_CLAN_UNOFFICIAL;
            case T_CLAN_TW:
                return T_IS_TW_NON_BOX;
            case T_CLAN_ADVANCED:
                return T_IS_ADVANCED;
            case T_CLAN_EXPERIMENTAL:
                return T_IS_EXPERIMENTAL;
            case T_CLAN_UNOFFICIAL:
                return T_IS_UNOFFICIAL;
            default:
                return T_TECH_UNKNOWN;
        }
    }

    /**
     * Return the tech level of the given gyro. This is necessary because gyros
     * are systems and hence don't have MiscType entries.
     * 
     * @param gyroType
     * @param isClan
     * @param year
     * @return
     */
    public static int getGyroTechLevel(int gyroType, boolean isClan, int year) {
        switch (gyroType) {
            case Mech.GYRO_STANDARD:
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
                    return T_INTRO_BOXSET;
                }
            case Mech.GYRO_XL:
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

            case Mech.GYRO_COMPACT:
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

            case Mech.GYRO_HEAVY_DUTY:
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
            case Mech.GYRO_SUPERHEAVY:
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
            case Mech.GYRO_NONE:
                return T_ALLOWED_ALL;
        }

        return T_TECH_UNKNOWN;
    }

    /**
     * Return the tech level of the given cockpit. THis is necessary because
     * cockpits are systems and hence don't have MiscType entries.
     * 
     * @param cockpitType
     * @param entityType
     * @param isClan
     * @param year
     * @return
     */
    public static int getCockpitTechLevel(int cockpitType, long entityType,
            boolean isClan, int year) {
        if ((entityType & Entity.ETYPE_MECH) != 0) {
            switch (cockpitType) {
                case Mech.COCKPIT_STANDARD:
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
                        return T_INTRO_BOXSET;
                    }
                case Mech.COCKPIT_SMALL:
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
                case Mech.COCKPIT_PRIMITIVE:
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
                case Mech.COCKPIT_PRIMITIVE_INDUSTRIAL:
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
                case Mech.COCKPIT_INDUSTRIAL:
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
                case Mech.COCKPIT_TORSO_MOUNTED:
                    if (isClan) {
                        if (year <= 3055) {
                            return T_CLAN_UNOFFICIAL;
                        }
                        return T_CLAN_EXPERIMENTAL;
                    }
                    if (year <= 3053) {
                        return T_IS_UNOFFICIAL;
                    } else if (year <= 3075) {
                        return T_IS_EXPERIMENTAL;
                    } else if (year <= 3100) {
                        return T_IS_ADVANCED;
                    } else {
                        return T_IS_TW_NON_BOX;
                    }
                case Mech.COCKPIT_INTERFACE: // Clan Version
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
                case Mech.COCKPIT_COMMAND_CONSOLE:
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
                case Mech.COCKPIT_TRIPOD:
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

                case Mech.COCKPIT_SUPERHEAVY:
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
                case Mech.COCKPIT_SUPERHEAVY_TRIPOD:
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
                case Mech.COCKPIT_VRRP:
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
                case Mech.COCKPIT_QUADVEE:
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
                case Mech.COCKPIT_SUPERHEAVY_INDUSTRIAL:
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
                    return T_INTRO_BOXSET;
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
                } else if (year <=3025) {
                    return T_IS_UNOFFICIAL;
                } else {
                    return T_IS_ADVANCED;
                }

            }

        }
        return T_TECH_UNKNOWN;
    }
}
