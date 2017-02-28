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
}
