/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.units;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

import megamek.common.bays.Bay;
import megamek.common.equipment.Transporter;

/**
 * Classes that implement this interface have the ability to load, carry, drop, and unload Infantry units in the game.
 * This interface includes a subset of the Bay.java class methods and Transporter interface.
 *
 * @see Bay
 * @see Transporter
 */
public interface InfantryTransporter extends Serializable {

    /**
     * These methods are required for Infantry unit loading/unloading. In practice, all are provided by Bay() or
     * overridden in transports that can carry infantry.
     */
    boolean canLoad(Entity unit);

    List<Entity> getDroppableUnits();

    Vector<Entity> getLoadedUnits();

    double getUnused();

    double getUnusedSlots();

    String getTransporterType();

    void load(Entity unit);

    double spaceForUnit(Entity unit);

    default int getCurrentDoors() {
        return 0;
    }
}
