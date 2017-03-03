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
