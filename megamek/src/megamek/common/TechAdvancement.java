/**
 * * MegaMek - Copyright (C) 2017 - The MegaMek Team
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

import java.util.Arrays;

/**
 * Handles the progression of technology through prototype, production, extinction and reintroduction
 * phases. Calculates current rules level for IS or Clan.
 * 
 * @author Neoancient
 *
 */
public class TechAdvancement {
	
	/* Local versions of ITechnology constants for convenience */
	public static final int DATE_NONE = ITechnology.DATE_NONE;
	public static final int DATE_PS = ITechnology.DATE_PS;
    public static final int DATE_ES = ITechnology.DATE_ES;
	
	public static final int TECH_BASE_ALL  = ITechnology.TECH_BASE_ALL;
	public static final int TECH_BASE_IS   = ITechnology.TECH_BASE_IS;
	public static final int TECH_BASE_CLAN = ITechnology.TECH_BASE_CLAN;
	
    public static final int ERA_SL     = ITechnology.ERA_SL;
    public static final int ERA_SW     = ITechnology.ERA_SW;
    public static final int ERA_CLAN   = ITechnology.ERA_CLAN;
    public static final int ERA_DA     = ITechnology.ERA_DA;
	
	public static final int PROTOTYPE    = 0;
	public static final int PRODUCTION   = 1;
	public static final int COMMON       = 2;
	public static final int EXTINCT      = 3;
	public static final int REINTRODUCED = 4;

	/* Aliases for TechConstants rules levels that reflect tech advancement usage */
    public static final int RULES_TOURNAMENT     = TechConstants.T_SIMPLE_STANDARD;
	public static final int RULES_ADVANCED     = TechConstants.T_SIMPLE_ADVANCED;
	public static final int RULES_EXPERIMENTAL = TechConstants.T_SIMPLE_EXPERIMENTAL;
	public static final int RULES_UNAVAILABLE  = TechConstants.T_SIMPLE_UNOFFICIAL;
	
    private int techBase = TECH_BASE_ALL;
    private int[] isAdvancement = new int[5];
    private int[] clanAdvancement = new int[5];
    private boolean isIntroLevel = false; //Whether RULES_STANDARD should be considered T_INTRO_BOXSET.
    private boolean unofficial = false;
    private int techRating = ITechnology.RATING_C;
    private int[] availability = new int[ERA_DA + 1];
    
    public TechAdvancement() {
        Arrays.fill(isAdvancement, DATE_NONE);
        Arrays.fill(clanAdvancement, DATE_NONE);
    }
    
    public void setTechBase(int base) {
        techBase = base;
    }
    
    public int getTechBase() {
        return techBase;
    }
    
    public void setISAdvancement(int[] prog) {
        Arrays.fill(isAdvancement, DATE_NONE);
        System.arraycopy(prog, 0, isAdvancement, 0, Math.min(isAdvancement.length, prog.length));
    }
    
    public void setClanAdvancement(int[] prog) {
        Arrays.fill(clanAdvancement, DATE_NONE);
        System.arraycopy(prog, 0, clanAdvancement, 0, Math.min(clanAdvancement.length, prog.length));
    }
    
    public void setAdvancement(int[] prog) {
        setISAdvancement(prog);
        setClanAdvancement(prog);
    }
    
    public void setISAdvancement(int prototype, int production, int common,
            int extinct, int reintroduction) {
        isAdvancement[PROTOTYPE] = prototype;
        isAdvancement[PRODUCTION] = production;
        isAdvancement[COMMON] = common;
        isAdvancement[EXTINCT] = extinct;
        isAdvancement[REINTRODUCED] = reintroduction;
    }

    public void setISAdvancement(int prototype, int production, int common,
            int extinct) {
        setISAdvancement(prototype, production, common, extinct, DATE_NONE);
    }

    public void setISAdvancement(int prototype, int production, int common) {
        setISAdvancement(prototype, production, common, DATE_NONE, DATE_NONE);
    }

    public void setISAdvancement(int prototype, int production) {
        setISAdvancement(prototype, production, DATE_NONE, DATE_NONE, DATE_NONE);
    }

    public void setISAdvancement(int prototype) {
        setISAdvancement(prototype, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE);
    }
    
    public void setClanAdvancement(int prototype, int production, int common,
            int extinct, int reintroduction) {
        clanAdvancement[PROTOTYPE] = prototype;
        clanAdvancement[PRODUCTION] = production;
        clanAdvancement[COMMON] = common;
        clanAdvancement[EXTINCT] = extinct;
        clanAdvancement[REINTRODUCED] = reintroduction;
    }

    public void setClanAdvancement(int prototype, int production, int common,
            int extinct) {
        setClanAdvancement(prototype, production, common, extinct, DATE_NONE);
    }

    public void setClanAdvancement(int prototype, int production, int common) {
        setClanAdvancement(prototype, production, common, DATE_NONE, DATE_NONE);
    }

    public void setClanAdvancement(int prototype, int production) {
        setClanAdvancement(prototype, production, DATE_NONE, DATE_NONE, DATE_NONE);
    }

    public void setClanAdvancement(int prototype) {
        setClanAdvancement(prototype, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE);
    }
    
    public void setAdvancement(int prototype, int production, int common,
            int extinct, int reintroduction) {
        setISAdvancement(prototype, production, common, extinct, reintroduction);
        setClanAdvancement(prototype, production, common, extinct, reintroduction);
    }
        
    public void setAdvancement(int prototype, int production, int common,
            int extinct) {
        setISAdvancement(prototype, production, common, extinct, DATE_NONE);
        setClanAdvancement(prototype, production, common, extinct, DATE_NONE);
    }
    
    public void setAdvancement(int prototype, int production, int common) {
        setISAdvancement(prototype, production, common, DATE_NONE, DATE_NONE);
        setClanAdvancement(prototype, production, common, DATE_NONE, DATE_NONE);
    }
    
    public void setAdvancement(int prototype, int production) {
        setISAdvancement(prototype, production, DATE_NONE, DATE_NONE, DATE_NONE);
        setClanAdvancement(prototype, production, DATE_NONE, DATE_NONE, DATE_NONE);
    }
    
    public void setAdvancement(int prototype) {
        setISAdvancement(prototype, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE);
        setClanAdvancement(prototype, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE);
    }
    
    public int getPrototypeDate(boolean clan) {
        return clan?clanAdvancement[PROTOTYPE] : isAdvancement[PROTOTYPE];
    }

    public int getProductionDate(boolean clan) {
        return clan?clanAdvancement[PRODUCTION] : isAdvancement[PRODUCTION];
    }

    public int getCommonDate(boolean clan) {
        return clan?clanAdvancement[COMMON] : isAdvancement[COMMON];
    }

    public int getExtinctionDate(boolean clan) {
        return clan?clanAdvancement[EXTINCT] : isAdvancement[EXTINCT];
    }

    public int getReintroductionDate(boolean clan) {
        return clan?clanAdvancement[REINTRODUCED] : isAdvancement[REINTRODUCED];
    }
    
    public int getIntroductionDate(boolean clan) {
        if (getPrototypeDate(clan) > 0) {
            return getPrototypeDate(clan);
        }
        if (getProductionDate(clan) > 0) {
            return getProductionDate(clan);
        }
        return getCommonDate(clan);
    }
    
    /*
     * Methods which return universe-wide dates
     */
    
    public int getPrototypeDate() {
        return earliestDate(isAdvancement[PROTOTYPE], clanAdvancement[PROTOTYPE]);
    }
    
    public int getProductionDate() {
        return earliestDate(isAdvancement[PRODUCTION], clanAdvancement[PRODUCTION]);
    }
    
    public int getCommonDate() {
        return earliestDate(isAdvancement[COMMON], clanAdvancement[COMMON]);
    }
    
    public int getExtinctionDate() {
        if (isAdvancement[EXTINCT] == DATE_NONE
                || clanAdvancement[EXTINCT] == DATE_NONE) {
            return DATE_NONE;
        }
        return Math.max(isAdvancement[EXTINCT], clanAdvancement[EXTINCT]);
    }
    
    public int getReintroductionDate() {
        /* check for universal extinction first */
        if (isAdvancement[EXTINCT] == DATE_NONE
                || clanAdvancement[EXTINCT] == DATE_NONE) {
            return DATE_NONE;
        }
        return earliestDate(isAdvancement[REINTRODUCED], clanAdvancement[REINTRODUCED]);
    }
    
    public int getIntroductionDate() {
        if (getPrototypeDate() > 0) {
            return getPrototypeDate();
        }
        if (getProductionDate() > 0) {
            return getProductionDate();
        }
        return getCommonDate();
    }
    
    /**
     * Finds the earliest of two dates, ignoring DATE_NA unless both values are set to DATE_NA
     */
    private static int earliestDate(int d1, int d2) {
        if (d1 < 0) {
            return d2;
        }
        if (d2 < 0) {
            return d1;
        }
        return Math.min(d1, d2);
    }
    
    public boolean isIntroLevel() {
        return isIntroLevel;
    }

    public void setIntroLevel(boolean intro) {
        isIntroLevel = intro;
    }
    
    public boolean isUnofficial() {
        return unofficial;
    }
    
    public void setUnofficial(boolean unofficial) {
        this.unofficial = unofficial;
    }
    
    public void setTechRating(int rating) {
        techRating = rating;
    }
    
    public int getTechRating() {
        return techRating;
    }
    
    public void setAvailability(int[] av) {
        System.arraycopy(av, 0, availability, 0, Math.min(av.length, availability.length));
    }
    
    public void setAvailability(int sl, int sw, int clan, int da) {
        availability[ERA_SL] = sl;
        availability[ERA_SW] = sw;
        availability[ERA_CLAN] = clan;
        availability[ERA_DA] = da;
    }

    /**
     * Base availability code in the given year regardless of tech base
     */
    public int getBaseAvailability(int year) {
        int era = getTechEra(year);
        if (era < 0 || era > availability.length) {
            return ITechnology.RATING_X;
        }
        return availability[era];
    }
    
    public int getBaseEraAvailability(int era) {
        if (era < 0 || era > availability.length) {
            return ITechnology.RATING_X;
        }
        return availability[era];
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
    public String getEraAvailabilityName(int era, boolean clan) {
        switch (era) {
        case ERA_SL:
            return EquipmentType.getRatingName(getAvailability(2779, clan));
        case ERA_SW:
            if (getAvailability(2780, clan) != getAvailability(3049, clan)) {
                return EquipmentType.getRatingName(getAvailability(2780, clan))
                        + "/" + EquipmentType.getRatingName(getAvailability(3049, clan));                
            }
            return EquipmentType.getRatingName(getAvailability(3049, clan));
        case ERA_CLAN:
            return EquipmentType.getRatingName(getAvailability(3100, clan));
        case ERA_DA:
            return EquipmentType.getRatingName(getAvailability(3145, clan));
        }
        return "U";
    }
    
    /**
     * Computes availability code of equipment for IS factions in the given year, adjusting
     * for tech base
     */
    public int getAvailability(int year, boolean clan) {
        int era = getTechEra(year);
        if (clan) {
            if (techBase == TECH_BASE_IS
                    && era < ERA_CLAN
                    && isAdvancement[PROTOTYPE] >= 2780) {
                return ITechnology.RATING_X;
            } else {
                return getBaseEraAvailability(era);
            }            
        } else {
            if (techBase == TECH_BASE_CLAN) {
                if (era < ERA_CLAN) {
                    return ITechnology.RATING_X;
                } else {
                    return Math.min(ITechnology.RATING_X, getBaseAvailability(era) + 1);
                }
            } else if (techBase == TECH_BASE_ALL
                    && era == ERA_SW
                    && availability[era] >= ITechnology.RATING_E
                    && isAdvancement[EXTINCT] != DATE_NONE
                    && year > isAdvancement[EXTINCT]) {
                return Math.min(ITechnology.RATING_X, availability[era] + 1);
            } else {
                return getBaseEraAvailability(era);
            }
        }
    }
    
    /**
     * Determines rules level for either IS or Clan usage.
     */
    public int getRulesLevel(int year, boolean clan) {
        if (clan) {
            if (getIntroductionDate(clan) > year
                    || isExtinct(year, clan)) {
                return RULES_UNAVAILABLE;
            } else if (year >= clanAdvancement[COMMON]) {
                return RULES_TOURNAMENT;
            } else if (year >= clanAdvancement[PRODUCTION]) {
                return RULES_ADVANCED;
            } else {
                return RULES_EXPERIMENTAL;
            }            
        } else {
            if (getIntroductionDate(clan) > year
                    || isExtinct(year, clan)) {
                return RULES_UNAVAILABLE;
            } else if (year >= isAdvancement[COMMON]) {
                return RULES_TOURNAMENT;
            } else if (year >= isAdvancement[PRODUCTION]) {
                return RULES_ADVANCED;
            } else {
                return RULES_EXPERIMENTAL;
            }            
        }
    }
    
    /**
     * Determines general rules level, taking the lower value if different.
     */
    public int getRulesLevel(int year) {
        return Math.min(getRulesLevel(year, true), getRulesLevel(year, false));
    }
    
    /**
     * Calculates the TechConstants value for the equipment.
     */
    public int getTechLevel(int year, boolean clan) {
        if (unofficial) {
            return techBase == TECH_BASE_CLAN? TechConstants.T_CLAN_UNOFFICIAL : TechConstants.T_IS_UNOFFICIAL;
        }
        switch (getRulesLevel(year, clan)) {
        case RULES_TOURNAMENT:
            if (isIntroLevel) {
                return TechConstants.T_INTRO_BOXSET;
            } else {
                return techBase == TECH_BASE_CLAN? TechConstants.T_CLAN_TW : TechConstants.T_IS_TW_ALL;
            }
        case RULES_ADVANCED:
            return techBase == TECH_BASE_CLAN? TechConstants.T_CLAN_ADVANCED : TechConstants.T_IS_ADVANCED;
        case RULES_EXPERIMENTAL:
            return techBase == TECH_BASE_CLAN? TechConstants.T_CLAN_EXPERIMENTAL : TechConstants.T_IS_EXPERIMENTAL;
        case RULES_UNAVAILABLE:
            return techBase == TECH_BASE_CLAN? TechConstants.T_CLAN_UNOFFICIAL : TechConstants.T_IS_UNOFFICIAL;
        }
        return TechConstants.T_TECH_UNKNOWN;
    }
    
    /**
     * Calculates the TechConstants value for the equipment. Uses IS constant for ALL if it
     * does not exist at that level.
     */
    public int getTechLevel(int year) {
        if (unofficial) {
            return techBase == TECH_BASE_CLAN? TechConstants.T_CLAN_UNOFFICIAL : TechConstants.T_IS_UNOFFICIAL;
        }
        switch (getRulesLevel(year)) {
        case RULES_TOURNAMENT:
            if (isIntroLevel) {
                return TechConstants.T_INTRO_BOXSET;
            } else if (techBase == TECH_BASE_ALL) {
                return TechConstants.T_TW_ALL;
            } else if (techBase == TECH_BASE_CLAN) {
                return TechConstants.T_CLAN_TW;
            } else {
                return TechConstants.T_IS_TW_ALL;
            }
        case RULES_ADVANCED:
            return techBase == TECH_BASE_CLAN? TechConstants.T_CLAN_ADVANCED : TechConstants.T_IS_ADVANCED;
        case RULES_EXPERIMENTAL:
            return techBase == TECH_BASE_CLAN? TechConstants.T_CLAN_EXPERIMENTAL : TechConstants.T_IS_EXPERIMENTAL;
        case RULES_UNAVAILABLE:
            return techBase == TECH_BASE_CLAN? TechConstants.T_CLAN_UNOFFICIAL : TechConstants.T_IS_UNOFFICIAL;
        }
        return TechConstants.T_TECH_UNKNOWN;
    }
    
    public boolean isExtinct(int year, boolean clan) {
        if (clan) {
            return clanAdvancement[EXTINCT] != DATE_NONE
                    && clanAdvancement[EXTINCT] < year
                    && (clanAdvancement[REINTRODUCED] == DATE_NONE
                            || year < clanAdvancement[REINTRODUCED]);
        } else {
            return isAdvancement[EXTINCT] != DATE_NONE
                    && isAdvancement[EXTINCT] < year
                    && (isAdvancement[REINTRODUCED] == DATE_NONE
                            || year < isAdvancement[REINTRODUCED]);
        }
    }
    
    public boolean isExtinct(int year) {
        return getExtinctionDate() != DATE_NONE
                && getExtinctionDate() < year
                && (getReintroductionDate() == DATE_NONE
                        || year < getReintroductionDate());
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
}
