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
import java.util.StringJoiner;

/**
 * Handles the progression of technology through prototype, production, extinction and reintroduction
 * phases. Calculates current rules level for IS or Clan.
 * 
 * @author Neoancient
 *
 */
public class TechAdvancement implements ITechnology {	
	
	public static final int PROTOTYPE    = 0;
	public static final int PRODUCTION   = 1;
	public static final int COMMON       = 2;
	public static final int EXTINCT      = 3;
	public static final int REINTRODUCED = 4;

    private int techBase = TECH_BASE_ALL;
    private int[] isAdvancement = new int[5];
    private int[] clanAdvancement = new int[5];
    private boolean[] isApproximate = new boolean[5];
    private boolean[] clanApproximate = new boolean[5];
    private int[] prototypeFactions = {};
    private int[] productionFactions = {};
    private int[] extinctionFactions = {};
    private int[] reintroductionFactions = {};
    private boolean isIntroLevel = false; //Whether RULES_STANDARD should be considered T_INTRO_BOXSET.
    private boolean unofficial = false;
    private int techRating = RATING_C;
    private int[] availability = new int[ERA_DA + 1];
    
    public TechAdvancement() {
        Arrays.fill(isAdvancement, DATE_NONE);
        Arrays.fill(clanAdvancement, DATE_NONE);
    }
    
    public TechAdvancement(int techBase) {
        this();
        this.techBase = techBase;
    }
    
    public TechAdvancement setTechBase(int base) {
        techBase = base;
        return this;
    }
    
    public int getTechBase() {
        return techBase;
    }
    
    public TechAdvancement setISAdvancement(int... prog) {
        Arrays.fill(isAdvancement, DATE_NONE);
        System.arraycopy(prog, 0, isAdvancement, 0, Math.min(isAdvancement.length, prog.length));
        return this;
    }
    
    public TechAdvancement setISApproximate(boolean... approx) {
        Arrays.fill(isApproximate, false);
        System.arraycopy(approx, 0, isApproximate, 0, Math.min(isApproximate.length, approx.length));
        return this;
    }
    
    public TechAdvancement setClanAdvancement(int... prog) {
        Arrays.fill(clanAdvancement, DATE_NONE);
        System.arraycopy(prog, 0, clanAdvancement, 0, Math.min(clanAdvancement.length, prog.length));
        return this;
    }
    
    public TechAdvancement setClanApproximate(boolean... approx) {
        Arrays.fill(clanApproximate, false);
        System.arraycopy(approx, 0, clanApproximate, 0, Math.min(clanApproximate.length, approx.length));
        return this;
    }
    
    public TechAdvancement setAdvancement(int... prog) {
        setISAdvancement(prog);
        setClanAdvancement(prog);
        return this;
    }
    
    public TechAdvancement setApproximate(boolean... approx) {
        setISApproximate(approx);
        setClanApproximate(approx);
        return this;
    }
    
    public TechAdvancement setPrototypeFactions(int... factions) {
        prototypeFactions = factions;
        return this;
    }
    
    public int[] getPrototypeFactions() {
        return prototypeFactions;
    }

    public TechAdvancement setProductionFactions(int... factions) {
        productionFactions = factions;
        return this;
    }

    public int[] getProductionFactions() {
        return productionFactions;
    }

    public TechAdvancement setExtinctionFactions(int... factions) {
        extinctionFactions = factions;
        return this;
    }

    public int[] getExtinctionFactions() {
        return extinctionFactions;
    }

    public TechAdvancement setReintroductionFactions(int... factions) {
        reintroductionFactions = factions;
        return this;
    }

    public int[] getReintroductionFactions() {
        return reintroductionFactions;
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

    /**
     * If the tech base is IS or Clan, returns the extinction date that matches the tech base. Otherwise
     * returns the later of the IS and Clan dates, or DATE_NONE if the tech has not gone extinct for both.
     * 
     * @return Universe-wide extinction date.
     */
    public int getExtinctionDate() {
        if (getTechBase() != TECH_BASE_ALL) {
            return getExtinctionDate(getTechBase() == TECH_BASE_CLAN);
        }
        if (isAdvancement[EXTINCT] == DATE_NONE
                || clanAdvancement[EXTINCT] == DATE_NONE) {
            return DATE_NONE;
        }
        return Math.max(isAdvancement[EXTINCT], clanAdvancement[EXTINCT]);
    }
    
    public int getReintroductionDate() {
        if (getTechBase() != TECH_BASE_ALL) {
            return getReintroductionDate(getTechBase() == TECH_BASE_CLAN);
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
    
    private String formatDate(int index, boolean clan, int[] factions) {
        int date = clan? clanAdvancement[index] : isAdvancement[index];
        if (date == DATE_NONE) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        if (clan? clanApproximate[index] : isApproximate[index]) {
            sb.append("~");
        }
        if (date == DATE_PS) {
            sb.append("PS");
        } else if (date == DATE_ES) {
            sb.append("ES");
        } else {
            sb.append(date);
        }
        if (factions != null && factions.length > 0) {
            StringJoiner sj = new StringJoiner(",");
            for (int f : factions) {
                if ((clan && f >= F_CLAN) || (!clan && f < F_CLAN)) {
                    sj.add(IO_FACTION_CODES[f]);
                }
            }
            if (sj.length() > 0) {
                sb.append("(").append(sj.toString()).append(")");
            }
        }
        return sb.toString();
    }
    
    /**
     * Formats prototype date indicating approximate when appropriate, and prototype faction if any
     * for either IS or Clan use tech base.
     */
    public String getPrototypeDateName(boolean clan) {
        return formatDate(PROTOTYPE, clan, prototypeFactions);
    }
    
    /**
     * Formats earliest of Clan or IS prototype date indicating approximate when appropriate,
     * and prototype faction if any for mixed tech.
     */
    public String getPrototypeDateName() {
        return formatDate(PROTOTYPE, clanAdvancement[PROTOTYPE] != DATE_NONE
                && clanAdvancement[PROTOTYPE] < isAdvancement[PROTOTYPE], prototypeFactions);
    }
    
    /**
     * Formats production date indicating approximate when appropriate, and production faction if any
     * for either IS or Clan use tech base.
     */
    public String getProductionDateName(boolean clan) {
        return formatDate(PRODUCTION, clan, productionFactions);
    }
    
    /**
     * Formats earliest of Clan or IS production date indicating approximate when appropriate,
     * and production faction if any for mixed tech.
     */
    public String getProductionDateName() {
        return formatDate(PRODUCTION, clanAdvancement[PRODUCTION] != DATE_NONE
                && clanAdvancement[PRODUCTION] < isAdvancement[PRODUCTION], productionFactions);
    }
    
    /**
     * Formats common date indicating approximate when appropriate.
     */
    public String getCommonDateName(boolean clan) {
        return formatDate(COMMON, clan, null);
    }
    
    /**
     * Formats earliest of Clan or IS common date indicating approximate when appropriate for mixed tech.
     */
    public String getCommonDateName() {
        return formatDate(COMMON, clanAdvancement[COMMON] != DATE_NONE
                && clanAdvancement[COMMON] < isAdvancement[COMMON], null);
    }
    
    /**
     * Formats extinction date indicating approximate when appropriate, and extinction faction if any
     * for either IS or Clan use tech base.
     */
    public String getExtinctionDateName(boolean clan) {
        return formatDate(EXTINCT, clan, extinctionFactions);
    }
    
    /**
     * Formats earliest of Clan or IS extinction date indicating approximate when appropriate,
     * and extinction faction if any for mixed tech.
     */
    public String getExtinctionDateName() {
        return formatDate(EXTINCT, clanAdvancement[EXTINCT] != DATE_NONE
                && clanAdvancement[EXTINCT] < isAdvancement[EXTINCT], extinctionFactions);
    }
    
    /**
     * Formats reintroduction date indicating approximate when appropriate, and reintroduction faction if any
     * for either IS or Clan use tech base.
     */
    public String getReintroductionDateName(boolean clan) {
        return formatDate(REINTRODUCED, clan, reintroductionFactions);
    }
    
    /**
     * Formats earliest of Clan or IS reintroduction date indicating approximate when appropriate,
     * and reintroduction faction if any for mixed tech.
     */
    public String getReintroductionDateName() {
        return formatDate(REINTRODUCED, clanAdvancement[REINTRODUCED] != DATE_NONE
                && clanAdvancement[REINTRODUCED] < isAdvancement[REINTRODUCED], reintroductionFactions);
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
    
    public TechAdvancement setAvailability(int... av) {
        System.arraycopy(av, 0, availability, 0, Math.min(av.length, availability.length));
        return this;
    }
    
    public TechAdvancement setAvailability(int era, int av) {
        if (era > 0 && era < availability.length) {
            availability[era] = av;
        }
        return this;
    }

    public int getBaseAvailability(int era) {
        if (era < 0 || era > availability.length) {
            return RATING_X;
        }
        return availability[era];
    }

    @Override
    public boolean isClan() {
        return techBase == TECH_BASE_CLAN;
    }

    @Override
    public boolean isMixedTech() {
        return techBase == TECH_BASE_ALL;
    }
}
