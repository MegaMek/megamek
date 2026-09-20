/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.common.units;

/** How the current prone posture began. Presentation metadata; {@link Entity#isProne()} owns the rules state. */
public enum ProneCause {
    NONE,
    VOLUNTARY,
    FORCED,
    UNKNOWN
}
