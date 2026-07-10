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

import java.util.List;

import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import megamek.common.units.Tank;

/**
 * Central logic for the facing a weapon fires its forward arc against, taking into account torso twist / secondary
 * facing and any rotatable mount: Mek shoulder/head/quad turrets, vehicle dual turrets, and Directional Torso Mounts
 * (BMM p.83). Keeping this in one place ensures the firing arc, the to-hit computation and the field-of-fire overlay
 * all agree in every phase.
 */
public final class TurretFacing {

    private TurretFacing() {}

    /**
     * Computes the facing (0-5) that a weapon's forward arc is measured against. This folds in the unit's facing or
     * secondary facing (torso twist), a Mek turret's facing offset, a vehicle dual turret's facing, and a Directional
     * Torso Mount's facing offset.
     *
     * @param attacker the firing unit
     * @param weaponId the equipment number of the weapon
     *
     * @return the facing the weapon's forward arc is measured against
     */
    public static int weaponFacing(Entity attacker, int weaponId) {
        int facing = attacker.isSecondaryArcWeapon(weaponId) ? attacker.getSecondaryFacing() : attacker.getFacing();
        Mounted<?> weapon = attacker.getEquipment(weaponId);

        if ((attacker instanceof Tank tank) && (weapon.getLocation() == tank.getLocTurret2())) {
            facing = tank.getDualTurretFacing();
        }

        if (weapon.isMekTurretMounted()) {
            facing = attacker.getSecondaryFacing() + (weapon.getFacing() % 6);
        }

        // A Directional Torso Mount (BMM p.83) aims its forward arc relative to the unit's facing plus the mount's
        // facing offset, so it rotates like a turret (2-point: front/rear; 3-point quad: any of the six).
        if (weapon.hasDirectionalTorsoMount()) {
            facing = (facing + weapon.getDirectionalMountFacing()) % 6;
        }
        return facing;
    }

    /**
     * @param entity the unit carrying the weapon
     * @param weapon the weapon to test
     *
     * @return {@code true} if the weapon sits on a rotatable mount whose facing the player can change - a Mek
     *       shoulder/head/quad turret, a vehicle main or dual (front) turret, or a Directional Torso Mount
     */
    public static boolean isRotatable(Entity entity, Mounted<?> weapon) {
        return isInTankMainTurret(entity, weapon)
              || isInTankDualTurret(entity, weapon)
              || weapon.isMekTurretMounted()
              || weapon.hasDirectionalTorsoMount();
    }

    /**
     * @param mounted the equipment item to test
     *
     * @return {@code true} if the item is a Mek turret (a shoulder, head or quad turret)
     */
    public static boolean isMekTurretItem(Mounted<?> mounted) {
        return mounted.getType().hasFlag(MiscType.F_SHOULDER_TURRET)
              || mounted.getType().hasFlag(MiscType.F_HEAD_TURRET)
              || mounted.getType().hasFlag(MiscType.F_QUAD_TURRET);
    }

    /**
     * @param entity the unit carrying the weapon
     * @param weapon the weapon to test
     *
     * @return {@code true} if the weapon sits in a vehicle's main turret - the turret that follows the unit's secondary
     *       facing, so rotating it is a turret twist. On a dual-turret vehicle this is the rear turret.
     */
    public static boolean isInTankMainTurret(Entity entity, Mounted<?> weapon) {
        return (entity instanceof Tank tank)
              && !tank.hasNoTurret()
              && (weapon.getLocation() == tank.getLocTurret());
    }

    /**
     * @param entity the unit carrying the weapon
     * @param weapon the weapon to test
     *
     * @return {@code true} if the weapon sits in a dual-turret vehicle's second (front) turret, whose facing is a
     *       freely-set offset independent of the turret twist
     */
    public static boolean isInTankDualTurret(Entity entity, Mounted<?> weapon) {
        return (entity instanceof Tank tank)
              && !tank.hasNoDualTurret()
              && (weapon.getLocation() == tank.getLocTurret2());
    }

    /**
     * Returns the facing offsets (relative to the unit's facing) that a Directional Torso Mount weapon may be set to
     * (BMM p.83): the 2-point mount allows only forward (0) and rear (3); the 3-point quad turret allows all six.
     *
     * @param weapon the directional-mount weapon
     *
     * @return the legal facing offsets, or an empty list if the weapon is not a directional mount
     */
    public static List<Integer> legalDirectionalMountOffsets(Mounted<?> weapon) {
        if (!weapon.hasDirectionalTorsoMount()) {
            return List.of();
        }
        return weapon.hasDirectional360TorsoMount() ? List.of(0, 1, 2, 3, 4, 5) : List.of(0, 3);
    }
}
