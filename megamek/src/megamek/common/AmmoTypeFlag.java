/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.common;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

public enum AmmoTypeFlag {

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

    private final int flagPosition;

    AmmoTypeFlag(int flagPosition) {
        this.flagPosition = flagPosition;
    }

    public int getFlagPosition() {
        return flagPosition;
    }

    public static AmmoTypeFlag fromFlagPosition(int flagPosition) {
        for (AmmoTypeFlag flag : values()) {
            if (flag.getFlagPosition() == flagPosition) {
                return flag;
            }
        }
        throw new IllegalArgumentException("No AmmoTypeFlag found for flag position " + flagPosition);
    }

    public EquipmentFlagBitSet asEquipmentFlagSet() {
        EquipmentFlagBitSet bitSet = new EquipmentFlagBitSet();
        bitSet.set(flagPosition);
        return bitSet;
    }

    public EquipmentFlagBitSet not() {
        var bitSet = asEquipmentFlagSet();
        bitSet.flip(0, AmmoTypeFlag.values().length);
        return bitSet;
    }

    public static Set<AmmoTypeFlag> fromBitSet(BitSet bitSet) {
        Set<AmmoTypeFlag> flags = new HashSet<>();
        for (AmmoTypeFlag flag : values()) {
            if (bitSet.get(flag.getFlagPosition())) {
                flags.add(flag);
            }
        }
        return flags;
    }

    public boolean isSet(BitSet bitSet) {
        return bitSet.get(flagPosition);
    }

    public static EquipmentFlagBitSet fromSet(Set<AmmoTypeFlag> flags) {
        EquipmentFlagBitSet bitSet = new EquipmentFlagBitSet();
        for (AmmoTypeFlag flag : flags) {
            bitSet.set(flag.getFlagPosition());
        }
        return bitSet;
    }

}
