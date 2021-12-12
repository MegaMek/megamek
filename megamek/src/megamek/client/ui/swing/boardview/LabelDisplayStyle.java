/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.boardview;

/** The style of unit label display (full, shortened, show the nickname, etc.) */
public enum LabelDisplayStyle { 
    FULL("Chassis and model"), 
    ABBREV("Abbreviated chassis and model / Meks only model"), 
    NICKNAME("Pilot or unit nickname  / chassis"), 
    CHASSIS("Chassis only"), 
    ONLY_NICKNAME("Only pilot or unit nickname"), 
    ONLY_STATUS("No labels");
    
    LabelDisplayStyle(String d) {
        description = d;
    }
    
    public final String description;
    
    public LabelDisplayStyle next() { 
        switch (this) {
            case FULL: 
                return ABBREV;
            case ABBREV: 
                return CHASSIS;
            case CHASSIS: 
                return NICKNAME;
            case NICKNAME: 
                return ONLY_NICKNAME;
            case ONLY_NICKNAME: 
                return ONLY_STATUS;
            default: 
                return FULL;
        }
    }
}
