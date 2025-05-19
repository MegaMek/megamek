/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common;

/**
 * Set of flags that can be used to determine how the ammo is used and its
 * special properties
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
