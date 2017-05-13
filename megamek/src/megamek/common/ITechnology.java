/*
 * MegaMek - Copyright (C) 2017 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
package megamek.common;

/**
 * Implemented by any class that is subject to tech advancement (entities, equipment, systems, etc.)
 * 
 * @author Neoancient
 *
 */
public interface ITechnology {
    
    public static final int TECH_BASE_ALL  = 0;
    public static final int TECH_BASE_IS   = 1;
    public static final int TECH_BASE_CLAN = 2;
    
    public static final int RATING_A = 0;
    public static final int RATING_B = 1;
    public static final int RATING_C = 2;
    public static final int RATING_D = 3;
    public static final int RATING_E = 4;
    public static final int RATING_F = 5;
    public static final int RATING_FSTAR = 6; // Increasing F (Clan equipmment for IS or extinct equipment
                                              // during SW era has a 50% chance of being X, denoted by F*.
    public static final int RATING_X = 7;

    public static final String[] ratingNames = { "A", "B", "C", "D", "E", "F", "F*",
    "X" };

    public static final int ERA_SL   = 0;
    public static final int ERA_SW   = 1;
    public static final int ERA_CLAN = 2;
    public static final int ERA_DA   = 3;
    public static final int ERA_NUM  = 4;

    public static final int DATE_NONE = -1;
    public static final int DATE_PS = 1950;
    public static final int DATE_ES = 2100;
    
    //codes for recording which factions had access to technology at various points
    public static final int F_IS = 0;
    public static final int F_CC = 1;
    public static final int F_CF = 2;
    public static final int F_CP = 3;
    public static final int F_CS = 4;
    public static final int F_DC = 5;
    public static final int F_EI = 6;
    public static final int F_FR = 7;
    public static final int F_FS = 8;
    public static final int F_FW = 9;
    public static final int F_LC = 10;
    public static final int F_MC = 11;
    public static final int F_MH = 12;
    public static final int F_OA = 13;
    public static final int F_TA = 14;
    public static final int F_TC = 15;
    public static final int F_TH = 16;
    public static final int F_RD = 17;
    public static final int F_RS = 18;
    public static final int F_RA = 19;
    public static final int F_RW = 20;
    public static final int F_WB = 21;
    public static final int F_MERC = 22;
    public static final int F_CLAN = 23;
    public static final int F_CBR = 24;
    public static final int F_CBS = 25;
    public static final int F_CCY = 26;
    public static final int F_CCC = 27;
    public static final int F_CFM = 28;
    public static final int F_CGB = 29;
    public static final int F_CGS = 30;
    public static final int F_CHH = 31;
    public static final int F_CIH = 32;
    public static final int F_CJF = 33;
    public static final int F_CMN = 34;
    public static final int F_CNC = 35;
    public static final int F_CSF = 36;
    public static final int F_CSJ = 37;
    public static final int F_CSR = 38;
    public static final int F_CSV = 39;
    public static final int F_CSA = 40;
    public static final int F_CWM = 41;
    public static final int F_CWF = 42;
    public static final int F_CWX = 43;
    public static final int F_CWV = 44;
    public static final int F_PER = 45;
    
    //display codes using values from IOps
    public static final String[] IO_FACTION_CODES = {
            "IS", "CC", "CF", "CP", "CS", "DC", "EI", "FR", "FS", "FW", "LC", "MC",
            "MH", "OA", "TA", "TC", "TH", "RD", "RS", "RA", "RW", "WB", "Merc", 
            "Clan", "CBR", "CBS", "CCY", "CCC", "CFM", "CGB", "CGS", "CHH", "CIH", "CJF", "CMN",
            "CNC", "CSF", "CSJ", "CSR", "CSV", "CSA", "CWM", "CWF", "CWX", "CWV", "PER" 
    };
    
    //faction lookup names for MHQ
    public static final String[] MM_FACTION_CODES = {
            "IS", "CC", "CIR", "CDP", "CS", "DC", "CEI", "FRR", "FS", "FWL", "LA", "MOC",
            "MH", "OA", "TA", "TC", "TH", "RD", "ROS", "RA", "RWR", "WOB", "MERC",
            "CLAN", "CB", "CBS", "CCO", "CCC", "CFM", "CGB", "CGS", "CHH", "CIH", "CJF", "CMG",
            "CNC", "CDS", "CSJ", "CSR", "CSV", "CSA", "CWI", "CW", "CWIE", "CWOV", "PER" 
    };
    
    boolean isClan();
    boolean isMixedTech();
    boolean isIntroLevel();
    boolean isUnofficial();
    default int getTechBase() {
        return isClan()? TECH_BASE_CLAN : TECH_BASE_IS;
    }
    
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
    default int getPrototypeDate(boolean clan) {
        return getPrototypeDate();
    }
    default int getProductionDate(boolean clan) {
        return getProductionDate();
    }
    default int getCommonDate(boolean clan) {
        return getCommonDate();
    }
    default int getExtinctionDate(boolean clan) {
        return getExtinctionDate();
    }
    default int getReintroductionDate(boolean clan) {
        return getReintroductionDate();
    }
    
    public static int getTechEra(int year) {
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
        if (isUnofficial()) {
            return clan? TechConstants.T_CLAN_UNOFFICIAL : TechConstants.T_IS_UNOFFICIAL;
        } else if (year >= getCommonDate(clan)) {
            if (clan) {
                return TechConstants.T_CLAN_TW;
            } else {
                return isIntroLevel()? TechConstants.T_INTRO_BOXSET : TechConstants.T_IS_TW_NON_BOX;
            }
        } else if (year >= getProductionDate(clan)) {
            return clan? TechConstants.T_CLAN_ADVANCED : TechConstants.T_IS_ADVANCED;
        } else if (year >= getPrototypeDate(clan)) {
            return clan? TechConstants.T_CLAN_EXPERIMENTAL : TechConstants.T_IS_EXPERIMENTAL;
        } else {
            return TechConstants.T_TECH_UNKNOWN;
        }
    }
    
    default int getTechLevel(int year) {
        return getTechLevel(year, isClan());
    }
    
    /**
     * Finds the lowest rules level the equipment qualifies for, for either IS or Clan faction
     * using it.
     * 
     * @param clan - whether tech level is being calculated for a Clan faction
     * @return - the lowest tech level available to the item
     */
    default int findMinimumRulesLevel(boolean clan) {
        if (getCommonDate(clan) != DATE_NONE) {
            return isIntroLevel()? TechConstants.T_SIMPLE_INTRO : TechConstants.T_SIMPLE_STANDARD;
        }
        if (getProductionDate(clan) != DATE_NONE) {
            return TechConstants.T_SIMPLE_ADVANCED;
        }
        if (getPrototypeDate(clan) != DATE_NONE) {
            return TechConstants.T_SIMPLE_EXPERIMENTAL;
        }
        return TechConstants.T_SIMPLE_UNOFFICIAL;
    }

    /**
     * Finds the lowest rules level the equipment qualifies for regardless of faction using it.
     * 
     * @return - the lowest tech level available to the item
     */
    default int findMinimumRulesLevel() {
        if (getCommonDate() != DATE_NONE) {
            return isIntroLevel()? TechConstants.T_SIMPLE_INTRO : TechConstants.T_SIMPLE_STANDARD;
        }
        if (getProductionDate() != DATE_NONE) {
            return TechConstants.T_SIMPLE_ADVANCED;
        }
        if (getPrototypeDate() != DATE_NONE) {
            return TechConstants.T_SIMPLE_EXPERIMENTAL;
        }
        return TechConstants.T_SIMPLE_UNOFFICIAL;
    }

    default boolean isExtinct(int year, boolean clan) {
        return getExtinctionDate(clan) != DATE_NONE
                && getExtinctionDate(clan) < year
                && (getReintroductionDate(clan) == DATE_NONE
                || year < getReintroductionDate(clan));
    }
    
    default boolean isExtinct(int year) {
        return getExtinctionDate() != DATE_NONE
                && getExtinctionDate() < year
                && (getReintroductionDate() == DATE_NONE
                || year < getReintroductionDate());        
    }
    
    default boolean isAvailableIn(int year, boolean clan) {
        return year >= getIntroductionDate(clan) && !isExtinct(year, clan);
    }
    default boolean isAvailableIn(int year) {
        return year >= getIntroductionDate() && !isExtinct(year);
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
        if (!clanUse && !isClan() && getTechEra(year) == ERA_SW
                && getBaseAvailability(ERA_SW) >= RATING_E
                && getExtinctionDate(false) != DATE_NONE
                && getExtinctionDate(false) <= year) {
            return Math.min(getBaseAvailability(ERA_SW) + 1, RATING_X);
        }
        return calcEraAvailability(getTechEra(year), clanUse);
    }
    
    /**
     * Adjusts base availability code for IS/Clan and IS extinction
     * 
     * @param era - one of the ERA_* constants from EquipmentType
     * @param clan - whether this should be calculated for a Clan faction rather than IS
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

    public static String getRatingName(int rating) {
        if ((rating < 0) || (rating > ratingNames.length)) {
            return "U";
        }
        return ratingNames[rating];
    }
    
    public static String getDateRange(int startIncl, int endNonIncl) {
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
