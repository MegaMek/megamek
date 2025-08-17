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

package megamek.common.equipment;

/**
 * Set of flags that can be used to determine how the ammo is used and its special properties
 *
 * @author Luana Coppio
 */
public enum AmmoTypeFlag implements EquipmentFlag {

    F_BATTLEARMOR, // only used by BA squads
    F_PROTOMEK, // only used by ProtoMeks

    F_ENCUMBERING, // Encumbering ammo - if loaded on a BA it cant jump or make anti-mek attacks until dumped

    F_MG, // Machinegun ammo
    F_MML_LRM, // LRM type
    F_MML_SRM, // SRM type

    F_HOTLOAD, // Ammo can be hotloaded

    F_SCREEN, // Used by MHQ for loading ammo bins

    F_INTERNAL_BOMB, // Used for Internal Bomb Bay bombs; to differentiate them from other bombs
    F_GROUND_BOMB, // the ammo can be used to ground bomb
    F_OTHER_BOMB, // For tag, rl pods, missiles and the like
    F_SPACE_BOMB, // defines that the ammo can be used to space bomb

    F_AR10_BARRACUDA, // barracuda type
    F_AR10_KILLER_WHALE, // Killer Whale type
    F_AR10_WHITE_SHARK, // White shark type
    F_CAP_MISSILE, // Other Capital-Missile
    F_CRUISE_MISSILE, // Used by MHQ for loading ammo bins
    F_TELE_MISSILE, // Tele-Missile

    F_NUCLEAR, // Nuclear missile
    F_SANTA_ANNA, // Nuke Santa Anna Missile
    F_PEACEMAKER, // Nuke Peacemaker Missile
    ;

}
