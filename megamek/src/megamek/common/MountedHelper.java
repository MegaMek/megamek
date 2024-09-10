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
package megamek.common;

import megamek.common.annotations.Nullable;

/**
 * Helper methods for Mounted to clean up checks like "is this an Artemis IV".
 *
 * @author Juliez
 */
public class MountedHelper {

    /** Returns true if the given Mounted m is a Coolant Pod. */
    public static boolean isCoolantPod(@Nullable Mounted<?> m) {
        return (m != null) && (m.getType() instanceof AmmoType)
                && (((AmmoType) m.getType()).getAmmoType() == AmmoType.T_COOLANT_POD);
    }

    /** Returns true if the given Mounted m is an Artemis IV system (IS/C). */
    public static boolean isArtemisIV(@Nullable Mounted<?> m) {
        return (m != null) && (m.getType() instanceof MiscType)
                && m.getType().hasFlag(MiscType.F_ARTEMIS);
    }

    /** Returns true if the given Mounted m is an Artemis V system. */
    public static boolean isArtemisV(@Nullable Mounted<?> m) {
        return (m != null) && (m.getType() instanceof MiscType)
                && m.getType().hasFlag(MiscType.F_ARTEMIS_V);
    }

    /** Returns true if the given Mounted m is a Proto Artemis system. */
    public static boolean isArtemisProto(@Nullable Mounted<?> m) {
        return (m != null) && (m.getType() instanceof MiscType)
                && m.getType().hasFlag(MiscType.F_ARTEMIS_PROTO);
    }

    /**
     * Returns true if the given Mounted m is any Artemis system (IV, V, Proto,
     * IS/C).
     */
    public static boolean isAnyArtemis(@Nullable Mounted<?> m) {
        return isArtemisIV(m) || isArtemisV(m) || isArtemisProto(m);
    }

}
