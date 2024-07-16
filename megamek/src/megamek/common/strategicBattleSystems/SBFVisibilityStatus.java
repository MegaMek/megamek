
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
package megamek.common.strategicBattleSystems;

/**
 * This enum represents the various states of visibility used in SBF games with the Recon rules,
 * IO BF p.195.
 */
public enum SBFVisibilityStatus {

    UNKNOWN,

    //
    INVISIBLE,

    // ?
    SENSOR_GHOST,

    // ?
    SENSOR_PING,

    // Solid Lock, Where?, Scan Interference:
    BLIP,

    // TP
    SOMETHING_OUT_THERE,

    // TP and SZ
    I_GOT_SOMETHING,

    // TP, SZ, MP
    PARTIAL_SCAN,

    // Same as Partial Scan? TP, SZ, MP, Elements
    EYES_ON_TARGET,

    // Same as Partial Scan? TP, SZ, MP, Elements
    PARTIAL_SCAN_RECON,

    // Full Scan
    VISIBLE;

    public SBFVisibilityStatus betterOf(SBFVisibilityStatus other) {
        return ordinal() > other.ordinal() ? this : other;
    }
}
