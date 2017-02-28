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
