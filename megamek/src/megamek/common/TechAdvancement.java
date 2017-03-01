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
    
    public static final int RATING_A    = ITechnology.RATING_A;
    public static final int RATING_B    = ITechnology.RATING_B;
    public static final int RATING_D    = ITechnology.RATING_C;
    public static final int RATING_C    = ITechnology.RATING_D;
    public static final int RATING_E    = ITechnology.RATING_E;
    public static final int RATING_F    = ITechnology.RATING_F;
    public static final int RATING_FSTAR    = ITechnology.RATING_FSTAR;
    public static final int RATING_X    = ITechnology.RATING_X;
	
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
    private boolean[] isApproximate = new boolean[5];
    private boolean[] clanApproximate = new boolean[5];
    private boolean isIntroLevel = false; //Whether RULES_STANDARD should be considered T_INTRO_BOXSET.
    private boolean unofficial = false;
    private int techRating = ITechnology.RATING_C;
    private int[] availability = new int[ERA_DA + 1];
    
    public TechAdvancement() {
        Arrays.fill(isAdvancement, DATE_NONE);
        Arrays.fill(clanAdvancement, DATE_NONE);
    }
    
    public TechAdvancement(int techBase) {
        this();
        this.techBase = techBase;
    }
    
    public TechAdvancement(int techBase, int[] isAdvancement, int[] clanAdvancement,
            int techRating, int[] availability) {
        this();
        setTechBase(techBase);
        if (isAdvancement == null) {
            Arrays.fill(isAdvancement, DATE_NONE);
        } else {
            setISAdvancement(isAdvancement);
        }
        if (clanAdvancement == null) {
            Arrays.fill(clanAdvancement, DATE_NONE);
        } else {
            setClanAdvancement(clanAdvancement);
        }
        setTechRating(techRating);
        setAvailability(availability);
    }
    
    public TechAdvancement(int[] advancement, int techRating, int[] availability) {
        this(TECH_BASE_ALL, advancement, advancement, techRating, availability);
    }
    
    public TechAdvancement setTechBase(int base) {
        techBase = base;
        return this;
    }
    
    public int getTechBase() {
        return techBase;
    }
    
    public TechAdvancement setISAdvancement(int[] prog) {
        Arrays.fill(isAdvancement, DATE_NONE);
        System.arraycopy(prog, 0, isAdvancement, 0, Math.min(isAdvancement.length, prog.length));
        return this;
    }
    
    public TechAdvancement setISApproximate(boolean[] approx) {
        Arrays.fill(isApproximate, false);
        System.arraycopy(approx, 0, isApproximate, 0, Math.min(isApproximate.length, approx.length));
        return this;
    }
    
    public TechAdvancement setClanAdvancement(int[] prog) {
        Arrays.fill(clanAdvancement, DATE_NONE);
        System.arraycopy(prog, 0, clanAdvancement, 0, Math.min(clanAdvancement.length, prog.length));
        return this;
    }
    
    public TechAdvancement setClanApproximate(boolean[] approx) {
        Arrays.fill(clanApproximate, false);
        System.arraycopy(approx, 0, clanApproximate, 0, Math.min(clanApproximate.length, approx.length));
        return this;
    }
    
    public TechAdvancement setAdvancement(int[] prog) {
        setISAdvancement(prog);
        setClanAdvancement(prog);
        return this;
    }
    
    public TechAdvancement setAdvancement(boolean[] approx) {
        setISApproximate(approx);
        setClanApproximate(approx);
        return this;
    }
    
    public TechAdvancement setISAdvancement(int prototype, int production, int common,
            int extinct, int reintroduction) {
        isAdvancement[PROTOTYPE] = prototype;
        isAdvancement[PRODUCTION] = production;
        isAdvancement[COMMON] = common;
        isAdvancement[EXTINCT] = extinct;
        isAdvancement[REINTRODUCED] = reintroduction;
        return this;
    }

    public TechAdvancement setISApproximate(boolean prototype, boolean production, boolean common,
            boolean extinct, boolean reintroduction) {
        isApproximate[PROTOTYPE] = prototype;
        isApproximate[PRODUCTION] = production;
        isApproximate[COMMON] = common;
        isApproximate[EXTINCT] = extinct;
        isApproximate[REINTRODUCED] = reintroduction;
        return this;
    }

    public TechAdvancement setISAdvancement(int prototype, int production, int common,
            int extinct) {
        setISAdvancement(prototype, production, common, extinct, DATE_NONE);
        return this;
    }

    public TechAdvancement setISApproximate(boolean prototype, boolean production, boolean common,
            boolean extinct) {
        setISApproximate(prototype, production, common, extinct, false);
        return this;
    }
    
    public TechAdvancement setISAdvancement(int prototype, int production, int common) {
        setISAdvancement(prototype, production, common, DATE_NONE, DATE_NONE);
        return this;
    }

    public TechAdvancement setISApproximate(boolean prototype, boolean production, boolean common) {
        setISApproximate(prototype, production, common, false, false);
        return this;
    }
    
    public TechAdvancement setISAdvancement(int prototype, int production) {
        setISAdvancement(prototype, production, DATE_NONE, DATE_NONE, DATE_NONE);
        return this;
    }

    public TechAdvancement setISApproximate(boolean prototype, boolean production) {
        setISApproximate(prototype, production, false, false, false);
        return this;
    }
    
    public TechAdvancement setISAdvancement(int prototype) {
        setISAdvancement(prototype, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE);
        return this;
    }
    
    public TechAdvancement setISApproximate(boolean prototype) {
        setISApproximate(prototype, false, false, false, false);
        return this;
    }
    
    public TechAdvancement setClanAdvancement(int prototype, int production, int common,
            int extinct, int reintroduction) {
        clanAdvancement[PROTOTYPE] = prototype;
        clanAdvancement[PRODUCTION] = production;
        clanAdvancement[COMMON] = common;
        clanAdvancement[EXTINCT] = extinct;
        clanAdvancement[REINTRODUCED] = reintroduction;
        return this;
    }

    public TechAdvancement setClanApproximate(boolean prototype, boolean production, boolean common,
            boolean extinct, boolean reintroduction) {
        clanApproximate[PROTOTYPE] = prototype;
        clanApproximate[PRODUCTION] = production;
        clanApproximate[COMMON] = common;
        clanApproximate[EXTINCT] = extinct;
        clanApproximate[REINTRODUCED] = reintroduction;
        return this;
    }

    public TechAdvancement setClanAdvancement(int prototype, int production, int common,
            int extinct) {
        setClanAdvancement(prototype, production, common, extinct, DATE_NONE);
        return this;
    }
    
    public TechAdvancement setClanApproximate(boolean prototype, boolean production, boolean common,
            boolean extinct) {
        setClanApproximate(prototype, production, common, extinct, false);
        return this;
    }

    public TechAdvancement setClanAdvancement(int prototype, int production, int common) {
        setClanAdvancement(prototype, production, common, DATE_NONE, DATE_NONE);
        return this;
    }

    public TechAdvancement setClanApproximate(boolean prototype, boolean production, boolean common) {
        setClanApproximate(prototype, production, common, false, false);
        return this;
    }

    public TechAdvancement setClanAdvancement(int prototype, int production) {
        setClanAdvancement(prototype, production, DATE_NONE, DATE_NONE, DATE_NONE);
        return this;
    }

    public TechAdvancement setClanApproximate(boolean prototype, boolean production) {
        setClanApproximate(prototype, production, false, false, false);
        return this;
    }

    public TechAdvancement setClanAdvancement(int prototype) {
        setClanAdvancement(prototype, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE);
        return this;
    }
    
    public TechAdvancement setClanApproximate(boolean prototype) {
        setClanApproximate(prototype, false, false, false, false);
        return this;
    }

    public TechAdvancement setAdvancement(int prototype, int production, int common,
            int extinct, int reintroduction) {
        setISAdvancement(prototype, production, common, extinct, reintroduction);
        setClanAdvancement(prototype, production, common, extinct, reintroduction);
        return this;
    }
        
    public TechAdvancement setApproximate(boolean prototype, boolean production, boolean common,
            boolean extinct, boolean reintroduction) {
        setISApproximate(prototype, production, common, extinct, reintroduction);
        setClanApproximate(prototype, production, common, extinct, reintroduction);
        return this;
    }
    
    public TechAdvancement setAdvancement(int prototype, int production, int common,
            int extinct) {
        setISAdvancement(prototype, production, common, extinct, DATE_NONE);
        setClanAdvancement(prototype, production, common, extinct, DATE_NONE);
        return this;
    }
    
    public TechAdvancement setApproximate(boolean prototype, boolean production, boolean common,
            boolean extinct) {
        setISApproximate(prototype, production, common, extinct, false);
        setClanApproximate(prototype, production, common, extinct, false);
        return this;
    }
    
    public TechAdvancement setAdvancement(int prototype, int production, int common) {
        setISAdvancement(prototype, production, common, DATE_NONE, DATE_NONE);
        setClanAdvancement(prototype, production, common, DATE_NONE, DATE_NONE);
        return this;
    }
    
    public TechAdvancement setApproximate(boolean prototype, boolean production, boolean common) {
        setISApproximate(prototype, production, common, false, false);
        setClanApproximate(prototype, production, common, false, false);
        return this;
    }
    
    public TechAdvancement setAdvancement(int prototype, int production) {
        setISAdvancement(prototype, production, DATE_NONE, DATE_NONE, DATE_NONE);
        setClanAdvancement(prototype, production, DATE_NONE, DATE_NONE, DATE_NONE);
        return this;
    }
    
    public TechAdvancement setApproximate(boolean prototype, boolean production) {
        setISApproximate(prototype, production, false, false, false);
        setClanApproximate(prototype, production, false, false, false);
        return this;
    }
    
    public TechAdvancement setAdvancement(int prototype) {
        setISAdvancement(prototype, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE);
        setClanAdvancement(prototype, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE);
        return this;
    }
    
    public TechAdvancement setApproximate(boolean prototype) {
        setISApproximate(prototype, false, false, false, false);
        setClanApproximate(prototype, false, false, false, false);
        return this;
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
    public static int earliestDate(int d1, int d2) {
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

    public TechAdvancement setIntroLevel(boolean intro) {
        isIntroLevel = intro;
        return this;
    }
    
    public boolean isUnofficial() {
        return unofficial;
    }
    
    public TechAdvancement setUnofficial(boolean unofficial) {
        this.unofficial = unofficial;
        return this;
    }
    
    public TechAdvancement setTechRating(int rating) {
        techRating = rating;
        return this;
    }
    
    public int getTechRating() {
        return techRating;
    }
    
    public TechAdvancement setAvailability(int[] av) {
        System.arraycopy(av, 0, availability, 0, Math.min(av.length, availability.length));
        return this;
    }
    
    public TechAdvancement setAvailability(int era, int av) {
        if (era > 0 && era < availability.length) {
            availability[era] = av;
        }
        return this;
    }
    
    public TechAdvancement setAvailability(int sl, int sw, int clan, int da) {
        availability[ERA_SL] = sl;
        availability[ERA_SW] = sw;
        availability[ERA_CLAN] = clan;
        availability[ERA_DA] = da;
        return this;
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
     * Set a minimum year. Used for composite items (e.g. Entity) that have their own introduction year
     * @param year
     */
    public void setMinYear(int year) {
        for (int i = 0; i < isAdvancement.length; i++) {
            if (isAdvancement[i] != DATE_NONE) {
                isAdvancement[i] = Math.max(isAdvancement[i], year);
            }
            if (clanAdvancement[i] != DATE_NONE) {
                clanAdvancement[i] = Math.max(clanAdvancement[i], year);
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
    
    public String experimentalDates(boolean clan) {
        int[] adv = clan? clanAdvancement : isAdvancement;
        boolean[] approx = clan? clanApproximate : isApproximate;
        if (adv[PROTOTYPE] == DATE_NONE || (adv[PRODUCTION] != DATE_NONE && adv[PRODUCTION] <= adv[PROTOTYPE])) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        if (approx[PROTOTYPE]) {
            sb.append("~");
        }
        sb.append(adv[PROTOTYPE]);
        if (adv[PRODUCTION] != DATE_NONE && adv[PRODUCTION] <= adv[COMMON]) {
            sb.append("-");
            if (approx[PRODUCTION]) {
                sb.append("~");
            }
            sb.append(adv[PRODUCTION] - 1);
        } else if (adv[COMMON] != DATE_NONE) {
            sb.append("-");
            if (approx[COMMON]) {
                sb.append("~");
            }
            sb.append(adv[COMMON] - 1);
        } else {
            sb.append("+");
        }
        return sb.toString();
    }
    
    public String advancedDates(boolean clan) {
        int[] adv = clan? clanAdvancement : isAdvancement;
        boolean[] approx = clan? clanApproximate : isApproximate;
        if (adv[PRODUCTION] == DATE_NONE || (adv[COMMON] != DATE_NONE && adv[COMMON] <= adv[PRODUCTION])) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        if (approx[PRODUCTION]) {
            sb.append("~");
        }
        sb.append(adv[PRODUCTION]);
        if (adv[COMMON] != DATE_NONE) {
            sb.append("-");
            if (approx[COMMON]) {
                sb.append("~");
            }
            sb.append(adv[COMMON] - 1);
        } else {
            sb.append("+");
        }
        return sb.toString();
    }
    
    public String standardDates(boolean clan) {
        int[] adv = clan? clanAdvancement : isAdvancement;
        boolean[] approx = clan? clanApproximate : isApproximate;
        if (adv[COMMON] == DATE_NONE) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        if (approx[COMMON]) {
            sb.append("~");
        }
        sb.append(adv[COMMON]).append("+");
        return sb.toString();
    }
    
    public String extinctDates(boolean clan) {
        int[] adv = clan? clanAdvancement : isAdvancement;
        boolean[] approx = clan? clanApproximate : isApproximate;
        if (adv[EXTINCT] == DATE_NONE) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        if (approx[EXTINCT]) {
            sb.append("~");
        }
        sb.append(adv[EXTINCT]);
        if (adv[REINTRODUCED] != DATE_NONE) {
            sb.append("-");
            if (approx[REINTRODUCED]) {
                sb.append("~");
            }
            sb.append(adv[REINTRODUCED]);
        } else {
            sb.append("+");
        }
        return sb.toString();
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
