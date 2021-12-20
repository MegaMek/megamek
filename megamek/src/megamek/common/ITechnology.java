/*
* MegaMek -
* Copyright (C) 2017 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.common;

/**
 * Implemented by any class that is subject to tech advancement (entities, equipment, systems, etc.)
 *
 * @author Neoancient
 */
public interface ITechnology {

    int TECH_BASE_ALL  = 0;
    int TECH_BASE_IS   = 1;
    int TECH_BASE_CLAN = 2;

    int RATING_A = 0;
    int RATING_B = 1;
    int RATING_C = 2;
    int RATING_D = 3;
    int RATING_E = 4;
    int RATING_F = 5;
    int RATING_FSTAR = 6; // Increasing F (Clan equipment for IS or extinct equipment
    // during SW era has a 50% chance of being X, denoted by F*.
    int RATING_X = 7;

    String[] ratingNames = { "A", "B", "C", "D", "E", "F", "F*", "X" };

    int ERA_SL   = 0;
    int ERA_SW   = 1;
    int ERA_CLAN = 2;
    int ERA_DA   = 3;
    int ERA_NUM  = 4;

    int DATE_NONE = -1;
    int DATE_PS = 1950;
    int DATE_ES = 2100;

    //codes for recording which factions had access to technology at various points
    int F_NONE = -1; // Indicates that factions should be ignored when calculating tech level.
    int F_IS = 0;
    int F_CC = 1;
    int F_CF = 2;
    int F_CP = 3;
    int F_CS = 4;
    int F_DC = 5;
    int F_EI = 6;
    int F_FC = 7;
    int F_FR = 8;
    int F_FS = 9;
    int F_FW = 10;
    int F_LC = 11;
    int F_MC = 12;
    int F_MH = 13;
    int F_OA = 14;
    int F_TA = 15;
    int F_TC = 16;
    int F_TH = 17;
    int F_RD = 18;
    int F_RS = 19;
    int F_RA = 20;
    int F_RW = 21;
    int F_WB = 22;
    int F_MERC = 23;
    int F_PER = 24;
    int F_CLAN = 25;
    int F_CBR = 26;
    int F_CBS = 27;
    int F_CCY = 28;
    int F_CCC = 29;
    int F_CFM = 30;
    int F_CGB = 31;
    int F_CGS = 32;
    int F_CHH = 33;
    int F_CIH = 34;
    int F_CJF = 35;
    int F_CMN = 36;
    int F_CNC = 37;
    int F_CSF = 38;
    int F_CSJ = 39;
    int F_CSR = 40;
    int F_CSV = 41;
    int F_CSA = 42;
    int F_CWM = 43;
    int F_CWF = 44;
    int F_CWX = 45;
    int F_CWV = 46;

    // display codes using values from IOps
    String[] IO_FACTION_CODES = {
            "IS", "CC", "CF", "CP", "CS", "DC", "EI", "FC", "FR", "FS", "FW", "LC", "MC",
            "MH", "OA", "TA", "TC", "TH", "RD", "RS", "RA", "RW", "WB", "Merc", "Per",
            "Clan", "CBR", "CBS", "CCY", "CCC", "CFM", "CGB", "CGS", "CHH", "CIH", "CJF", "CMN",
            "CNC", "CSF", "CSJ", "CSR", "CSV", "CSA", "CWM", "CWF", "CWX", "CWV"
    };

    // faction lookup names for MHQ
    String[] MM_FACTION_CODES = {
            "IS", "CC", "CIR", "CDP", "CS", "DC", "CEI", "FC", "FRR", "FS", "FWL", "LA", "MOC",
            "MH", "OA", "TA", "TC", "TH", "RD", "ROS", "RA", "RWR", "WOB", "MERC", "Periphery",
            "CLAN", "CB", "CBS", "CCO", "CCC", "CFM", "CGB", "CGS", "CHH", "CIH", "CJF", "CMG",
            "CNC", "CDS", "CSJ", "CSR", "CSV", "CSA", "CWI", "CW", "CWIE", "CWOV",
    };

    boolean isClan();
    boolean isMixedTech();
    int getTechBase();

    int getIntroductionDate();
    int getPrototypeDate();
    int getProductionDate();
    int getCommonDate();
    int getExtinctionDate();
    int getReintroductionDate();

    int getTechRating();
    int getBaseAvailability(int era);

    default int getIntroductionDate(boolean clan) {
        return getIntroductionDate();
    }
    int getIntroductionDate(boolean clan, int faction);
    default int getPrototypeDate(boolean clan) {
        return getPrototypeDate();
    }
    int getPrototypeDate(boolean clan, int faction);
    default int getProductionDate(boolean clan) {
        return getProductionDate();
    }
    int getProductionDate(boolean clan, int faction);
    default int getCommonDate(boolean clan) {
        return getCommonDate();
    }
    int getExtinctionDate(boolean clan, int faction);
    default int getExtinctionDate(boolean clan) {
        return getExtinctionDate();
    }
    int getReintroductionDate(boolean clan, int faction);
    default int getReintroductionDate(boolean clan) {
        return getReintroductionDate();
    }

    static int getTechEra(int year) {
        if (year < 2780) {
            return ERA_SL;
        } else if (year < 3050) {
            return ERA_SW;
        } else if (year < 3130) {
            return ERA_CLAN;
        } else {
            return ERA_DA;
        }
    }

    default int getTechLevel(int year, boolean clan) {
        return getSimpleLevel(year, clan).getCompoundTechLevel(clan);
    }

    default int getTechLevel(int year) {
        return getTechLevel(year, isClan());
    }

    default SimpleTechLevel getSimpleLevel(int year) {
        if (getSimpleLevel(year, true).compareTo(getSimpleLevel(year, false)) < 0) {
            return getSimpleLevel(year, true);
        }
        return getSimpleLevel(year, false);
    }

    default SimpleTechLevel getSimpleLevel(int year, boolean clan) {
        return getSimpleLevel(year, clan, F_NONE);
    }

    default SimpleTechLevel getSimpleLevel(int year, boolean clan, int faction) {
        if (isUnofficial()) {
            return SimpleTechLevel.UNOFFICIAL;
        } else if (year >= getCommonDate(clan) && getCommonDate(clan) != DATE_NONE) {
            if (isIntroLevel()) {
                return SimpleTechLevel.INTRO;
            } else {
                return SimpleTechLevel.STANDARD;
            }
        } else if (year >= getProductionDate(clan, faction) && getProductionDate(clan, faction) != DATE_NONE) {
            return SimpleTechLevel.ADVANCED;
        } else if (year >= getPrototypeDate(clan, faction) && getPrototypeDate(clan, faction) != DATE_NONE) {
            return SimpleTechLevel.EXPERIMENTAL;
        } else {
            return SimpleTechLevel.UNOFFICIAL;
        }
    }

    /**
     * For non-era-based usage, provide a single tech level that does not vary with date.
     *
     * @return The base rules level of the equipment or unit.
     */
    SimpleTechLevel getStaticTechLevel();

    default boolean isIntroLevel() {
        return getStaticTechLevel() == SimpleTechLevel.INTRO;
    }

    default boolean isUnofficial() {
        return getStaticTechLevel() == SimpleTechLevel.UNOFFICIAL;
    }


    /**
     * Finds the lowest rules level the equipment qualifies for, for either IS or Clan faction
     * using it.
     *
     * @param clan - whether tech level is being calculated for a Clan faction
     * @return - the lowest tech level available to the item
     */
    default SimpleTechLevel findMinimumRulesLevel(boolean clan) {
        if (getCommonDate(clan) != DATE_NONE) {
            return (getStaticTechLevel() == SimpleTechLevel.INTRO)
                    ? SimpleTechLevel.INTRO : SimpleTechLevel.STANDARD;
        }
        if (getProductionDate(clan) != DATE_NONE) {
            return SimpleTechLevel.ADVANCED;
        }
        if (getPrototypeDate(clan) != DATE_NONE) {
            return SimpleTechLevel.EXPERIMENTAL;
        }
        return SimpleTechLevel.UNOFFICIAL;
    }

    /**
     * Finds the lowest rules level the equipment qualifies for regardless of faction using it.
     *
     * @return - the lowest tech level available to the item
     */
    default SimpleTechLevel findMinimumRulesLevel() {
        if (getCommonDate() != DATE_NONE) {
            return (getStaticTechLevel() == SimpleTechLevel.INTRO)
                    ? SimpleTechLevel.INTRO : SimpleTechLevel.STANDARD;
        }
        if (getProductionDate() != DATE_NONE) {
            return SimpleTechLevel.ADVANCED;
        }
        if (getPrototypeDate() != DATE_NONE) {
            return SimpleTechLevel.EXPERIMENTAL;
        }
        return SimpleTechLevel.UNOFFICIAL;
    }

    default boolean isExtinct(int year, boolean clan) {
        return getExtinctionDate(clan) != DATE_NONE
                && getExtinctionDate(clan) < year
                && (getReintroductionDate(clan) == DATE_NONE
                || year < getReintroductionDate(clan));
    }

    default boolean isExtinct(int year, boolean clan, int faction) {
        // Tech that is lost but later recovered in the IS is not lost to ComStar.
        if ((F_CS == faction) && (getReintroductionDate(false) != DATE_NONE)) {
            return false;
        }
        return getExtinctionDate(clan) != DATE_NONE
                && getExtinctionDate(clan) < year
                && (getReintroductionDate(clan) == DATE_NONE
                || year < getReintroductionDate(clan, faction));
    }

    default boolean isExtinct(int year) {
        return getExtinctionDate() != DATE_NONE
                && getExtinctionDate() < year
                && (getReintroductionDate() == DATE_NONE
                || year < getReintroductionDate());
    }

    default boolean isAvailableIn(int year, boolean clan) {
        return year >= getIntroductionDate(clan) && getIntroductionDate(clan) != DATE_NONE  && !isExtinct(year, clan);
    }

    default boolean isAvailableIn(int year) {
        return year >= getIntroductionDate() && getIntroductionDate() != DATE_NONE && !isExtinct(year);
    }

    default boolean isAvailableIn(int year, boolean clan, int faction) {
        return year >= getIntroductionDate(clan, faction)
                && getIntroductionDate(clan, faction) != DATE_NONE  && !isExtinct(year, clan, faction);
    }

    default boolean isLegal(int year, int techLevel, boolean mixedTech) {
        return isLegal(year, SimpleTechLevel.convertCompoundToSimple(techLevel),
                TechConstants.isClan(techLevel), mixedTech);
    }

    default boolean isLegal(int year, SimpleTechLevel simpleRulesLevel, boolean clanBase, boolean mixedTech) {
        if (mixedTech) {
            if (!isAvailableIn(year)) {
                return false;
            } else {
                return getSimpleLevel(year).ordinal() <= simpleRulesLevel.ordinal();
            }
        } else {
            if (getTechBase() != TECH_BASE_ALL
                    && clanBase != isClan()) {
                return false;
            }
            if (!isAvailableIn(year, clanBase)) {
                return false;
            }
            return getSimpleLevel(year, clanBase).ordinal() <= simpleRulesLevel.ordinal();
        }
    }

    /**
     * Adjusts availability for certain combinations of era and IS/Clan use
     * @param era - one of the four tech eras
     * @param clanUse - whether the faction trying to obtain the tech is IS or Clan
     * @return - the adjusted availability code
     */
    default int calcEraAvailability(int era, boolean clanUse) {
        if (clanUse) {
            if (!isClan()
                    && era < ERA_CLAN
                    && getPrototypeDate(false) >= 2780) {
                return ITechnology.RATING_X;
            } else {
                return getBaseAvailability(era);
            }
        } else {
            if (isClan()) {
                if (era < ERA_CLAN) {
                    return ITechnology.RATING_X;
                } else {
                    return Math.min(ITechnology.RATING_X, getBaseAvailability(era) + 1);
                }
            } else {
                return getBaseAvailability(era);
            }
        }
    }

    default int calcYearAvailability(int year, boolean clanUse) {
        return calcYearAvailability(year, clanUse, -1);
    }

    default int calcYearAvailability(int year, boolean clanUse, int faction) {
        if (!clanUse && !isClan() && (faction != F_CS) && (getTechEra(year) == ERA_SW)
                && (getBaseAvailability(ERA_SW) >= RATING_E)
                && (getExtinctionDate(false) != DATE_NONE)
                && (getExtinctionDate(false) <= year)
                && ((getReintroductionDate(false) == DATE_NONE)
                        || (getReintroductionDate() > year))) {
            return Math.min(getBaseAvailability(ERA_SW) + 1, RATING_X);
        }
        return calcEraAvailability(getTechEra(year), clanUse);
    }

    /**
     * Adjusts base availability code for IS/Clan and IS extinction
     *
     * @param era - one of the ERA_* constants from EquipmentType
     * @param clanUse - whether this should be calculated for a Clan faction rather than IS
     * @return - The availability code for the faction in the era. The code for an IS faction
     *           during the SW era may be two values indicating availability before and after
     *           the extinction date.
     */
    default String getEraAvailabilityName(int era, boolean clanUse) {
        if (!clanUse && !isClan() && era == ERA_SW
                && getBaseAvailability(ERA_SW) >= RATING_E
                && getBaseAvailability(ERA_SW) < RATING_X
                && getExtinctionDate(false) != DATE_NONE
                && getTechEra(getExtinctionDate(false)) == ERA_SW) {
            return getRatingName(getBaseAvailability(ERA_SW))
                    + "(" + getRatingName(getBaseAvailability(ERA_SW) + 1) + ")";
        }
        return getRatingName(calcEraAvailability(era, clanUse));
    }

    default String getTechRatingName() {
        return getRatingName(getTechRating());
    }

    default String getEraAvailabilityName(int era) {
        return getEraAvailabilityName(era, isClan());
    }

    default String getFullRatingName(boolean clanUse) {
        String rating = getRatingName(getTechRating());
        rating += "/";
        rating += getEraAvailabilityName(ERA_SL, clanUse);
        rating += "-";
        rating += getEraAvailabilityName(ERA_SW, clanUse);
        rating += "-";
        rating += getEraAvailabilityName(ERA_CLAN, clanUse);
        rating += "-";
        rating += getEraAvailabilityName(ERA_DA, clanUse);
        return rating;
    }

    default String getFullRatingName() {
        return getFullRatingName(isClan());
    }

    default int calcEraAvailability(int era) {
        return calcEraAvailability(era, isClan());
    }

    default int calcYearAvailability(int year) {
        return calcYearAvailability(year, isClan());
    }

    static String getRatingName(int rating) {
        if ((rating < 0) || (rating >= ratingNames.length)) {
            return "U";
        }
        return ratingNames[rating];
    }

    static String getDateRange(int startIncl, int endNonIncl) {
        if (startIncl == DATE_NONE) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(startIncl);
        if (endNonIncl == DATE_NONE) {
            sb.append("+");
        } else if (endNonIncl > startIncl + 1) {
            sb.append("-").append(endNonIncl - 1);
        }
        return sb.toString();
    }

    default String getExperimentalRange(boolean clan) {
        return getDateRange(getPrototypeDate(clan), getProductionDate(clan));
    }
    default String getAdvancedRange(boolean clan) {
        return getDateRange(getProductionDate(clan), getCommonDate(clan));
    }
    default String getStandardRange(boolean clan) {
        return getDateRange(getCommonDate(clan), DATE_NONE);
    }
    default String getExtinctionRange(boolean clan) {
        return getDateRange(getExtinctionDate(clan), getReintroductionDate(clan));
    }

    default String getExperimentalRange() {
        return getDateRange(getPrototypeDate(), getProductionDate());
    }
    default String getAdvancedRange() {
        return getDateRange(getProductionDate(), getCommonDate());
    }
    default String getStandardRange() {
        return getDateRange(getCommonDate(), DATE_NONE);
    }
    default String getExtinctionRange() {
        return getDateRange(getExtinctionDate(), getReintroductionDate());
    }
}
