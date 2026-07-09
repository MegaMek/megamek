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
package megamek.client.ui.clientGUI;

import megamek.common.annotations.Nullable;

/**
 * How the client handles recording the combat-summary GIF of the minimap. Recording costs CPU for the whole game
 * (a minimap render and a GIF frame encode per phase), so consent is established before any work is done rather
 * than asking whether to keep the file at game end.
 */
public enum GifRecordingMode {

    /** Record every game without asking. */
    ALWAYS,

    /** Ask once at the start of each game whether to record it. The default. */
    ASK,

    /** Never record; no frames are rendered or encoded and no dialogs are shown. */
    NEVER;

    /**
     * Parses a stored preference value leniently.
     *
     * @param value The stored value, or {@code null}
     *
     * @return The matching mode, or {@link #ASK} for {@code null} or unrecognized values
     */
    public static GifRecordingMode parse(@Nullable String value) {
        if (value != null) {
            for (GifRecordingMode mode : values()) {
                if (mode.name().equalsIgnoreCase(value)) {
                    return mode;
                }
            }
        }
        return ASK;
    }
}
