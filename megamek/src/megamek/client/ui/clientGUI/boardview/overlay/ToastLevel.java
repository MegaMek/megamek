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
package megamek.client.ui.clientGUI.boardview.overlay;

import java.awt.Color;

import megamek.client.ui.clientGUI.GUIPreferences;

/**
 * Severity levels for board toast notifications. Each level defines a background color and how much longer than the
 * player's base display time its messages linger, so a more urgent level always outlasts a less urgent one no matter
 * where the player sets the base. Callers may override the duration outright for specific toasts.
 */
public enum ToastLevel {
    INFO(new Color(41, 98, 168), 0),
    SUCCESS(new Color(46, 125, 50), 0),
    WARNING(new Color(183, 134, 11), 1),
    ERROR(new Color(176, 42, 42), 2),
    /** A Game Master's action - purple, so it reads as the GM's hand rather than a game outcome. */
    GAMEMASTER(new Color(106, 61, 154), 1);

    private static final int MILLIS_PER_SECOND = 1000;

    private final Color backgroundColor;
    private final int durationOffsetSeconds;

    ToastLevel(Color backgroundColor, int durationOffsetSeconds) {
        this.backgroundColor = backgroundColor;
        this.durationOffsetSeconds = durationOffsetSeconds;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Returns this level's display time, being the player's configured base duration plus this level's urgency
     * offset. At the default base of 3 seconds this yields the long-standing 3s INFO/SUCCESS, 4s WARNING/GAMEMASTER
     * and 5s ERROR timings.
     *
     * @return the display time in milliseconds
     */
    public int getDefaultDurationMs() {
        int baseSeconds = GUIPreferences.getInstance().getToastDurationSeconds();
        return (baseSeconds + durationOffsetSeconds) * MILLIS_PER_SECOND;
    }
}
