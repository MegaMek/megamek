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

/**
 * Handles the progression of technology through prototype, production, extinction and reintroduction
 * phases. Calculates current rules level for IS or Clan.
 * 
 * @author Neoancient
 *
 */
public class TechProgression {
	
	/* Constants for special cases not involving a specific year */
	public static final int DATE_NA = -1;
	public static final int DATE_PS = 1950;
    public static final int DATE_ES = 2100;
	
	public static final int TECH_BASE_ALL  = 0;
	public static final int TECH_BASE_IS   = 1;
	public static final int TECH_BASE_CLAN = 2;

	/* Aliases for TechConstants rules levels that reflect tech progression usage */
    public static final int RULES_TOURNAMENT     = TechConstants.T_SIMPLE_STANDARD;
	public static final int RULES_ADVANCED     = TechConstants.T_SIMPLE_ADVANCED;
	public static final int RULES_EXPERIMENTAL = TechConstants.T_SIMPLE_EXPERIMENTAL;
	public static final int RULES_UNAVAILABLE  = TechConstants.T_SIMPLE_UNOFFICIAL;
	
    private int techBase = TECH_BASE_ALL;
    private int isPrototype = DATE_NA;
    private int isProduction = DATE_NA;
    private int isCommon = DATE_NA;
    private int isExtinction = DATE_NA;
    private int isReintroduction = DATE_NA;
    private int clanPrototype = DATE_NA;
    private int clanProduction = DATE_NA;
    private int clanCommon = DATE_NA;
    private int clanExtinction = DATE_NA;
    private int clanReintroduction = DATE_NA;
    private boolean isIntroLevel = false; //Whether RULES_STANDARD should be considered T_INTRO_BOXSET.
    private int techRating = EquipmentType.RATING_C;
    private int[] availability = new int[EquipmentType.ERA_DA + 1];
    
    public void setTechBase(int base) {
        techBase = base;
    }
    
    public int getTechBase() {
        return techBase;
    }
    
    public void setISProgression(int prototype, int production, int common,
            int extinct, int reintroduction) {
        isPrototype = prototype;
        isProduction = production;
        isCommon = common;
        isExtinction = extinct;
        isReintroduction = reintroduction;
    }

    public void setISProgression(int prototype, int production, int common,
            int extinct) {
        setISProgression(prototype, production, common, extinct, DATE_NA);
    }

    public void setISProgression(int prototype, int production, int common) {
        setISProgression(prototype, production, common, DATE_NA, DATE_NA);
    }

    public void setISProgression(int prototype, int production) {
        setISProgression(prototype, production, DATE_NA, DATE_NA, DATE_NA);
    }

    public void setISProgression(int prototype) {
        setISProgression(prototype, DATE_NA, DATE_NA, DATE_NA, DATE_NA);
    }
    
    public void setClanProgression(int prototype, int production, int common,
            int extinct, int reintroduction) {
        clanPrototype = prototype;
        clanProduction = production;
        clanCommon = common;
        clanExtinction = extinct;
        clanReintroduction = reintroduction;
    }

    public void setClanProgression(int prototype, int production, int common,
            int extinct) {
        setClanProgression(prototype, production, common, extinct, DATE_NA);
    }

    public void setClanProgression(int prototype, int production, int common) {
        setClanProgression(prototype, production, common, DATE_NA, DATE_NA);
    }

    public void setClanProgression(int prototype, int production) {
        setClanProgression(prototype, production, DATE_NA, DATE_NA, DATE_NA);
    }

    public void setClanProgression(int prototype) {
        setClanProgression(prototype, DATE_NA, DATE_NA, DATE_NA, DATE_NA);
    }
    
    public void setProgression(int prototype, int production, int common,
            int extinct, int reintroduction) {
        setISProgression(prototype, production, common, extinct, reintroduction);
        setClanProgression(prototype, production, common, extinct, reintroduction);
    }
        
    public void setProgression(int prototype, int production, int common,
            int extinct) {
        setISProgression(prototype, production, common, extinct, DATE_NA);
        setClanProgression(prototype, production, common, extinct, DATE_NA);
    }
    
    public void setProgression(int prototype, int production, int common) {
        setISProgression(prototype, production, common, DATE_NA, DATE_NA);
        setClanProgression(prototype, production, common, DATE_NA, DATE_NA);
    }
    
    public void setProgression(int prototype, int production) {
        setISProgression(prototype, production, DATE_NA, DATE_NA, DATE_NA);
        setClanProgression(prototype, production, DATE_NA, DATE_NA, DATE_NA);
    }
    
    public void setProgression(int prototype) {
        setISProgression(prototype, DATE_NA, DATE_NA, DATE_NA, DATE_NA);
        setClanProgression(prototype, DATE_NA, DATE_NA, DATE_NA, DATE_NA);
    }

    public int getISPrototype() {
        return isPrototype;
    }

    public void setISPrototype(int isPrototype) {
        this.isPrototype = isPrototype;
    }

    public int getISProduction() {
        return isProduction;
    }

    public void setISProduction(int isProduction) {
        this.isProduction = isProduction;
    }

    public int getISCommon() {
        return isCommon;
    }

    public void setISCommon(int isCommon) {
        this.isCommon = isCommon;
    }

    public int getISExtinction() {
        return isExtinction;
    }

    public void setISExtinction(int isExtinction) {
        this.isExtinction = isExtinction;
    }

    public int getISReintroduction() {
        return isReintroduction;
    }

    public void setISReintroduction(int isReintroduction) {
        this.isReintroduction = isReintroduction;
    }

    public int getClanPrototype() {
        return clanPrototype;
    }

    public void setClanPrototype(int clanPrototype) {
        this.clanPrototype = clanPrototype;
    }

    public int getClanProduction() {
        return clanProduction;
    }

    public void setClanProduction(int clanProduction) {
        this.clanProduction = clanProduction;
    }

    public int getClanCommon() {
        return clanCommon;
    }

    public void setClanCommon(int clanCommon) {
        this.clanCommon = clanCommon;
    }

    public int getClanExtinction() {
        return clanExtinction;
    }

    public void setClanExtinction(int clanExtinction) {
        this.clanExtinction = clanExtinction;
    }

    public int getClanReintroduction() {
        return clanReintroduction;
    }

    public void setClanReintroduction(int clanReintroduction) {
        this.clanReintroduction = clanReintroduction;
    }

    public void setIntroLevel(boolean intro) {
        isIntroLevel = intro;
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

    /**
     * Base availability code in the given year regardless of tech base
     */
    public int getBaseAvailability(int year) {
        int era = getTechEra(year);
        if (era < 0 || era > availability.length) {
            return EquipmentType.RATING_X;
        }
        return availability[era];
    }
    
    /**
     * Computes availability code of equipment for IS factions in the given year, adjusting
     * for tech base
     */
    public int getAvailability(int year, boolean clan) {
        int era = getTechEra(year);
        if (clan) {
            if (techBase == TECH_BASE_IS
                    && era < EquipmentType.ERA_CLAN
                    && isPrototype >= 2780) {
                return EquipmentType.RATING_X;
            } else {
                return getBaseAvailability(era);
            }            
        } else {
            if (techBase == TECH_BASE_CLAN) {
                if (era < EquipmentType.ERA_CLAN) {
                    return EquipmentType.RATING_X;
                } else {
                    return Math.min(EquipmentType.RATING_X, getBaseAvailability(era) + 1);
                }
            } else if (techBase == TECH_BASE_ALL
                    && era == EquipmentType.ERA_SW
                    && availability[era] >= EquipmentType.RATING_E
                    && isExtinction != DATE_NA
                    && year > isExtinction) {
                return Math.min(EquipmentType.RATING_X, availability[era] + 1);
            } else {
                return getBaseAvailability(era);
            }
        }
    }
    
    public int getRulesLevel(int year, boolean clan) {
        if (clan) {
            if (year < clanPrototype
                    || isExtinct(year, clan)) {
                return RULES_UNAVAILABLE;
            } else if (year >= clanCommon) {
                return RULES_TOURNAMENT;
            } else if (year >= clanProduction) {
                return RULES_ADVANCED;
            } else {
                return RULES_EXPERIMENTAL;
            }            
        } else {
            if (year < isPrototype
                    || isExtinct(year, clan)) {
                return RULES_UNAVAILABLE;
            } else if (year >= isCommon) {
                return RULES_TOURNAMENT;
            } else if (year >= isProduction) {
                return RULES_ADVANCED;
            } else {
                return RULES_EXPERIMENTAL;
            }            
        }
    }
    
    /**
     * Calculates the TechConstants value for the equipment.
     */
    public int getTechLevel(int year, boolean clan) {
        switch (getRulesLevel(year, clan)) {
        case RULES_TOURNAMENT:
            if (isIntroLevel) {
                return TechConstants.T_INTRO_BOXSET;
            } else {
                return clan? TechConstants.T_CLAN_TW : TechConstants.T_IS_TW_ALL;
            }
        case RULES_ADVANCED:
            return clan? TechConstants.T_CLAN_ADVANCED : TechConstants.T_IS_ADVANCED;
        case RULES_EXPERIMENTAL:
            return clan? TechConstants.T_CLAN_EXPERIMENTAL : TechConstants.T_IS_EXPERIMENTAL;
        case RULES_UNAVAILABLE:
            return clan? TechConstants.T_CLAN_UNOFFICIAL : TechConstants.T_IS_UNOFFICIAL;
        }
        return TechConstants.T_TECH_UNKNOWN;
    }
    
    public boolean isExtinct(int year, boolean clan) {
        if (clan) {
            return clanExtinction != DATE_NA
                    && clanExtinction > year
                    && (clanReintroduction == DATE_NA
                            || year < clanReintroduction);
        } else {
            return isExtinction != DATE_NA
                    && isExtinction > year
                    && (isReintroduction == DATE_NA
                            || year < isReintroduction);
        }
    }
    
    public static int getTechEra(int year) {
        if (year < 2780) {
            return EquipmentType.ERA_SL;
        } else if (year < 3050) {
            return EquipmentType.ERA_SW;
        } else if (year < 3130) {
            return EquipmentType.ERA_CLAN;
        } else {
            return EquipmentType.ERA_DA;
        }
    }
}
