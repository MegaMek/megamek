/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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
package megamek.common;

/**
 * This interface is for classes that represent combat units with battlefield roles (Sniper, Juggernaut,
 * Dogfighter...). Implemented by ASCardDisplayable (= MechSummary and AlphaStrikeElement) and Entity. Implementing
 * classes should make sure not to use null as a UnitRole, but at least UNDETERMINED.
 */
public interface CombatRole {

    /** @return The battlefield role (UnitRole) of this combat unit. */
    UnitRole getRole();

    /** @return True when this combat unit has a battlefield role which isn't UNDETERMINED or NONE. */
    default boolean hasRole() {
        return getRole().hasRole();
    }
}
