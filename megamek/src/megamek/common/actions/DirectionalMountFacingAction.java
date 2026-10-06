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

package megamek.common.actions;

import java.io.Serial;

/**
 * Sets the facing of a weapon's Directional Torso Mount (BMM p.83). The facing is an offset (0-5) from the unit's
 * facing - 0 = forward, 3 = rear; the 2-point mount uses only those two, the 3-point quad turret may use any. Like a
 * torso twist this is declared during the firing phase and rides inside the attack packet, but unlike a torso twist the
 * chosen facing persists across rounds until deliberately changed again.
 */
public class DirectionalMountFacingAction extends AbstractEntityAction {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int weaponNumber;
    private final int facing;

    /**
     * @param entityId     the acting entity's id
     * @param weaponNumber the equipment number of the Directional Torso Mount weapon to reface
     * @param facing       the chosen facing offset (0-5) from the unit's facing
     */
    public DirectionalMountFacingAction(int entityId, int weaponNumber, int facing) {
        super(entityId);
        this.weaponNumber = weaponNumber;
        this.facing = facing;
    }

    public int getWeaponNumber() {
        return weaponNumber;
    }

    public int getFacing() {
        return facing;
    }
}
