/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

package megamek.common.equipment;

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.Mounted;

public class AmmoMounted extends Mounted<AmmoType> {

    public AmmoMounted(Entity entity, AmmoType type) {
        super(entity, type);

        setShotsLeft(type.getShots());
        setSize(type.getTonnage(entity));
    }

    /**
     * Change the type of ammo in this bin
     * @param at The new ammo type
     */
    public void changeAmmoType(AmmoType at) {
        setType(at);
        if (getLocation() == Entity.LOC_NONE) {
            // Oneshot launcher
            setShotsLeft(1);
        } else {
            // Regular launcher
            setShotsLeft(at.getShots());
        }
    }


}
