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
 * MekWarrior, BattleMek, `Mek and AeroTek are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MekWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.clientGUI.boardview.gpu;

/**
 * How far one unit's arms are currently shown swung over from the front. Flipping arms arrives as a plain on or off;
 * this swings the shown angle toward it over a moment instead of jumping, the same way {@link UpperBodyTurn} handles a
 * torso twist. Presentation only: the game state already holds the firing arc.
 * <p>
 * The arms swing about the shoulder's own left-right axis, so they rise forward, pass over the shoulder and come to
 * rest pointing behind the unit. Half a turn leaves each arm occupying exactly the space it started in, mirrored
 * front to back, which is why a shoulder that carries its arm cleanly at rest also carries it cleanly when flipped.
 * </p>
 */
final class ArmFlip {
    /** Half a turn in half a second: quick enough to read as a weapon action, slow enough to follow. */
    static final float DEGREES_PER_SECOND = 360;
    private static final float FLIPPED_DEGREES = 180;

    private boolean started;
    private boolean flipped;
    private float degrees;

    /**
     * Moves the shown angle toward the wanted state. The first call snaps, so a unit that comes into view already
     * flipped does not swing into place.
     *
     * @param wantFlipped whether the arms should be shown pointing to the rear
     * @param seconds     the time since the last frame
     *
     * @return {@code true} if the shown angle changed and the model has to be posed again
     */
    boolean advance(boolean wantFlipped, float seconds) {
        float target = wantFlipped ? FLIPPED_DEGREES : 0;
        if (!started) {
            started = true;
            flipped = wantFlipped;
            degrees = target;
            return true;
        }
        flipped = wantFlipped;
        float remaining = target - degrees;
        if (remaining == 0) {
            return false;
        }
        float step = DEGREES_PER_SECOND * Math.max(0, seconds);
        degrees = (Math.abs(remaining) <= step) ? target : degrees + Math.signum(remaining) * step;
        return true;
    }

    /** @return whether the arms are wanted flipped, as last given to {@link #advance(boolean, float)} */
    boolean flipped() {
        return flipped;
    }

    /** @return the angle to show now, in degrees swung up and over from the front */
    float degrees() {
        return degrees;
    }
}
