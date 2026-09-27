/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.orders;

import megamek.common.units.Entity;

/**
 * Which units a player can give routes, formations and other unit orders to. Orders are for the ground map: units on
 * the ground, VTOLs and other low fliers, and - on a best-effort basis - conventional fighters and fixed-wing support
 * aircraft. Aerospace fighters, small craft, DropShips, larger craft, LandAirMeks in fighter mode and anything on a
 * space board take no orders.
 *
 * <p>The menus, the Move Order editor and the server all ask here, so a unit the menus hide is also refused when an
 * order for it is typed.</p>
 */
public final class OrderEligibility {

    private OrderEligibility() {}

    /**
     * @param entity a unit
     *
     * @return {@code true} if the unit is of a kind that can take orders where it is: anything but an aerospace unit,
     *       unless a conventional fighter or fixed-wing support aircraft, and nothing on a space board
     */
    public static boolean isOrderableKind(Entity entity) {
        if (entity.isSpaceborne()) {
            return false;
        }
        if (!entity.isAero()) {
            return true;
        }
        return entity.isConventionalFighter() || entity.isFixedWingSupport();
    }
}
