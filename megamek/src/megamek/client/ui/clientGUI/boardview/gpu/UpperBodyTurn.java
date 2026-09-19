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
package megamek.client.ui.clientGUI.boardview.gpu;

/**
 * How far one unit's upper body is currently shown turned from its legs. A torso twist arrives as a whole number of
 * hexsides; this swings the shown angle toward it over a moment instead of jumping, the way the board camera turns.
 * Presentation only: the game state already holds the new facing.
 */
final class UpperBodyTurn {
    /** One hexside in a quarter of a second, the same pace as a camera rotation step. */
    static final float DEGREES_PER_SECOND = 240;
    private static final float HEXSIDE_DEGREES = 60;

    private boolean started;
    private int hexsides;
    private float degrees;

    /**
     * Moves the shown angle toward the wanted twist by the shortest way round. The first call snaps, so a unit that
     * comes into view already twisted does not swing into place.
     *
     * @param targetHexsides the wanted twist in hexsides, clockwise positive
     * @param seconds        the time since the last frame
     *
     * @return {@code true} if the shown angle changed and the model has to be posed again
     */
    boolean advance(int targetHexsides, float seconds) {
        float target = targetHexsides * HEXSIDE_DEGREES;
        if (!started) {
            started = true;
            hexsides = targetHexsides;
            degrees = target;
            return true;
        }
        hexsides = targetHexsides;
        float remaining = shortestTurn(target - degrees);
        if (remaining == 0) {
            return false;
        }
        float step = DEGREES_PER_SECOND * Math.max(0, seconds);
        degrees = (Math.abs(remaining) <= step) ? target : degrees + Math.signum(remaining) * step;
        return true;
    }

    /** @return the wanted twist in hexsides, as last given to {@link #advance(int, float)} */
    int hexsides() {
        return hexsides;
    }

    /** @return the angle to show now, in degrees clockwise from the legs */
    float degrees() {
        return degrees;
    }

    /** @return the wanted twist in degrees clockwise from the legs */
    float targetDegrees() {
        return hexsides * HEXSIDE_DEGREES;
    }

    /** @return the same turn expressed from {@code -180} (exclusive) to {@code 180} degrees */
    static float shortestTurn(float turnDegrees) {
        float wrapped = turnDegrees % 360;
        if (wrapped > 180) {
            wrapped -= 360;
        } else if (wrapped <= -180) {
            wrapped += 360;
        }
        return wrapped;
    }
}
