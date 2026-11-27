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
package megamek.common.compute;

import megamek.common.equipment.WeaponType;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;
import megamek.common.units.Targetable;

/**
 * Decides which hit locations an aimed shot may name, for attacks that can only reach part of the target.
 * <p>
 * The aimed-shot roll (TW p.110) puts the hit on the named location before the attack's own hit table is consulted,
 * so a location the attack can never reach must not be named. The client uses this to enable the locations in the
 * aimed shot dialog, and the server uses it to drop an aimed location the attack cannot reach.
 * </p>
 */
public final class AimedShotLocations {

    private AimedShotLocations() {}

    /**
     * @param attacker   the unit making the attack
     * @param weaponType the weapon or infantry attack being made
     * @param target     the target of the attack
     * @param location   the location an aimed shot would name
     *
     * @return {@code true} if the attack can reach that location: a leg attack only reaches legs (TW p.220), and any
     *       attack by a unit swarming a two- or three-legged Mek rolls on the swarm table, which has no legs. Every
     *       other attack can reach any location.
     */
    public static boolean canAimAt(Entity attacker, WeaponType weaponType, Targetable target, int location) {
        if (!(target instanceof Entity targetEntity)) {
            return true;
        }
        if (Infantry.LEG_ATTACK.equals(weaponType.getInternalName())) {
            return targetEntity.locationIsLeg(location);
        }
        boolean isSwarmingTarget = (attacker.getSwarmTargetId() != Entity.NONE)
              && (attacker.getSwarmTargetId() == targetEntity.getId());
        boolean isSwarmTableWithoutLegs = (targetEntity instanceof Mek) && !(targetEntity instanceof QuadMek);
        if (isSwarmingTarget && isSwarmTableWithoutLegs) {
            return !targetEntity.locationIsLeg(location);
        }
        return true;
    }
}
