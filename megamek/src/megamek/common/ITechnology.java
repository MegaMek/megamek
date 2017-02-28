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
    
    TechAdvancement getTechAdvancement();

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

    public static final int ERA_SL = 0;
    public static final int ERA_SW = 1;
    public static final int ERA_CLAN = 2;
    public static final int ERA_DA = 3;

    public static final int DATE_NONE = -1;
    public static final int DATE_PS = 1950;
    public static final int DATE_ES = 2100;

    /* Convenience methods */
    default int getTechBase() {
        return getTechAdvancement().getTechBase();
    }
    default int getRulesLevel(int year, boolean clan) {
        return getTechAdvancement().getRulesLevel(year, clan);
    }
    default int getIntroductionDate(boolean clan) {
        return getTechAdvancement().getIntroductionDate(clan);
    }
    default int getExtinctionDate(boolean clan) {
        return getTechAdvancement().getExtinctionDate(clan);
    }
    default int getReintroductionDate(boolean clan) {
        return getTechAdvancement().getReintroductionDate(clan);
    }

    
    /**
     * Adjusts TechAdvancement dates of items made up of multiple components (e.g. Entity) to be
     * consistent with all subcomponents by choosing the latest date in each category except extinction,
     * which chooses the earliest date.
     * 
     * @param master - the object to be adjusted
     * @param component - the component to use for adjustment
     * @param mixed - whether the master item is mixed tech
     */
    public static void aggregate(ITechnology master, ITechnology component, boolean mixed) {
        aggregate(master, component.getTechAdvancement(), mixed);
    }
    
    /**
     * Adjusts TechAdvancement dates of items made up of multiple components (e.g. Entity) to be
     * consistent with all subcomponents by choosing the latest date in each category except extinction,
     * which chooses the earliest date.
     * 
     * @param master - the object to be adjusted
     * @param ta - the TechAdvancement object for the component
     * @param mixed - whether the master item is mixed tech
     */
    public static void aggregate(ITechnology master, TechAdvancement ta, boolean mixed) {
        if (mixed) {
            master.getTechAdvancement().setISAdvancement(
                    Math.max(master.getTechAdvancement().getPrototypeDate(),
                            ta.getPrototypeDate()),
                    Math.max(master.getTechAdvancement().getProductionDate(),
                            ta.getProductionDate()),
                    Math.max(master.getTechAdvancement().getCommonDate(),
                            ta.getCommonDate()),
                    TechAdvancement.earliestDate(master.getTechAdvancement().getExtinctionDate(),
                            ta.getExtinctionDate()),
                    Math.max(master.getTechAdvancement().getReintroductionDate(),
                            ta.getReintroductionDate()));            
        } else {
            if (master.getTechBase() == TECH_BASE_IS || master.getTechBase() == TECH_BASE_ALL) {
                master.getTechAdvancement().setISAdvancement(
                        Math.max(master.getTechAdvancement().getPrototypeDate(false),
                                ta.getPrototypeDate(false)),
                        Math.max(master.getTechAdvancement().getProductionDate(false),
                                ta.getProductionDate(false)),
                        Math.max(master.getTechAdvancement().getCommonDate(false),
                                ta.getCommonDate(false)),
                        TechAdvancement.earliestDate(master.getTechAdvancement().getExtinctionDate(false),
                                ta.getExtinctionDate(false)),
                        Math.max(master.getTechAdvancement().getReintroductionDate(false),
                                ta.getReintroductionDate(false)));
            }
            if (master.getTechBase() == TECH_BASE_CLAN || master.getTechBase() == TECH_BASE_ALL) {
                master.getTechAdvancement().setClanAdvancement(
                        Math.max(master.getTechAdvancement().getPrototypeDate(true),
                                ta.getPrototypeDate(true)),
                        Math.max(master.getTechAdvancement().getProductionDate(true),
                                ta.getProductionDate(true)),
                        Math.max(master.getTechAdvancement().getCommonDate(true),
                                ta.getCommonDate(true)),
                        TechAdvancement.earliestDate(master.getTechAdvancement().getExtinctionDate(true),
                                ta.getExtinctionDate(true)),
                        Math.max(master.getTechAdvancement().getReintroductionDate(true),
                                ta.getReintroductionDate(true)));
            }
        }
    }
}
