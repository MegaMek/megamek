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
 * @author Luana Coppio
 */
public enum AmmoTypeFlag implements IndexedFlag {

    // ammo flags
    F_MG(0),
    F_BATTLEARMOR(1), // only used by BA squads
    F_PROTOMEK(2), // only used by ProtoMeks
    F_HOTLOAD(3), // Ammo can be hotloaded

    // BA can't jump or make anti-mek until dumped
    F_ENCUMBERING(4),

    F_MML_LRM(5), // LRM type
    F_AR10_WHITE_SHARK(6), // White shark type
    F_AR10_KILLER_WHALE(7), // Killer Whale type
    F_AR10_BARRACUDA(8), // barracuda type
    F_NUCLEAR(9), // Nuclear missile
    F_SANTA_ANNA(14), // Santa Anna Missile
    F_PEACEMAKER(15), // Peacemaker Missile
    F_TELE_MISSILE(10), // Tele-Missile
    F_CAP_MISSILE(11), // Other Capital-Missile
    F_SPACE_BOMB(12), // can be used to space bomb

    // can be used to ground bomb
    F_GROUND_BOMB(13),
    F_MML_SRM(14), // SRM type

    // Numbers 14-15 out of order. See nuclear missiles, above

    // For tag, rl pods, missiles and the like
    F_OTHER_BOMB(16),

    // Used by MHQ for loading ammo bins
    F_CRUISE_MISSILE(17),

    // Used by MHQ for loading ammo bins
    F_SCREEN(18),

    // Used for Internal Bomb Bay bombs; to differentiate them from
    F_INTERNAL_BOMB(19);

    private final int flagIndex;

    AmmoTypeFlag(int flagIndex) {
        assert flagIndex >= 0;
        this.flagIndex = flagIndex;
    }

    @Override
    public int getFlagIndex() {
        return flagIndex;
    }

}
