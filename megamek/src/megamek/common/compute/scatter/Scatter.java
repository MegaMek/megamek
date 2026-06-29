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
package megamek.common.compute.scatter;

import megamek.common.board.Coords;
import megamek.common.compute.Compute;

/**
 * Scatter geometry for missed shots (artillery, bombs, and anything else that uses a scatter diagram). These are
 * stateless helpers over {@link Coords}; the methods that pick a direction or distance roll dice via
 * {@link Compute#d6(int)}, so they are random rather than deterministic. Nothing here is stored in game state, so no
 * serialization handling is required.
 *
 * <p>Standard scatter (TW) moves a single distance in one of the six directions. Advanced Scatter
 * (TO:AR) instead rolls dice for two legs two hexsides apart, so a missed shot can land on any hex rather than only on
 * the six straight-line spines.</p>
 */
public final class Scatter {

    /**
     * Hexes by which the Oblique Artilleryman and Golden Goose abilities reduce a missed shot's scatter distance, to a
     * minimum of {@code 0} (CamOps, 5th printing: Oblique Artilleryman p.78, Golden Goose p.75).
     */
    public static final int SPA_SCATTER_REDUCTION = 2;

    private Scatter() {}

    /**
     * @param marginOfFailure the attack's margin of failure (sign ignored)
     *
     * @return the standard scatter distance in hexes, which is the magnitude of the margin of failure
     */
    public static int standardDistance(int marginOfFailure) {
        return Math.abs(marginOfFailure);
    }

    /**
     * @param marginOfFailure the attack's margin of failure (sign ignored)
     * @param reduction       hexes to subtract (e.g. Oblique Artilleryman or Golden Goose)
     *
     * @return the standard scatter distance after the reduction, no less than {@code 0}
     */
    public static int reducedDistance(int marginOfFailure, int reduction) {
        return Math.max(standardDistance(marginOfFailure) - reduction, 0);
    }

    /**
     * @param marginOfFailure the attack's margin of failure (sign ignored)
     *
     * @return the number of d6 rolled for each leg of an Advanced Scatter, which is the margin of failure rounded up to
     *       the next even number then halved (1-2 -&gt; 1, 3-4 -&gt; 2, ...)
     */
    static int diceCount(int marginOfFailure) {
        return (Math.abs(marginOfFailure) + 1) / 2;
    }

    /**
     * Standard scatter in a random one of the six directions (artillery, dive bombs).
     *
     * @param target   the hex to scatter from
     * @param distance the scatter distance in hexes; its magnitude is used
     *
     * @return the scatter outcome
     */
    public static ScatterResult omnidirectional(Coords target, int distance) {
        return inDirection(target, Compute.d6(1) - 1, distance);
    }

    /**
     * Standard scatter into the front arc of a flight path (altitude bombing).
     *
     * @param target   the hex to scatter from
     * @param facing   the flight-path facing the bomb was dropped along
     * @param distance the scatter distance in hexes; its magnitude is used
     *
     * @return the scatter outcome
     */
    public static ScatterResult frontArc(Coords target, int facing, int distance) {
        int direction = switch (Compute.d6(1)) {
            case 1, 2 -> Math.floorMod(facing - 1, 6);
            case 5, 6 -> Math.floorMod(facing + 1, 6);
            default -> facing;
        };
        return inDirection(target, direction, distance);
    }

    /**
     * Advanced Scatter (TO:AR), any direction. Rolls {@link #diceCount} d6 for an initial leg in one of the six
     * directions, then a second leg of the same dice count two hexsides clockwise, so the shot can land on any hex.
     *
     * <p>A scatter-distance reduction (Oblique Artilleryman, Golden Goose) is applied to the initial
     * leg, since that leg is the analogue of the standard scatter distance.</p>
     *
     * @param target          the hex to scatter from
     * @param marginOfFailure the attack's margin of failure (sign ignored)
     * @param reduction       hexes to subtract from the initial leg; {@code 0} for none
     *
     * @return the scatter outcome
     */
    public static ScatterResult advanced(Coords target, int marginOfFailure, int reduction) {
        int dice = diceCount(marginOfFailure);
        int direction = Compute.d6(1) - 1;
        int firstLeg = Math.max(Compute.d6(dice) - reduction, 0);
        Coords intermediate = target.translated(direction, firstLeg);
        Coords landing = intermediate.translated((direction + 2) % 6, Compute.d6(dice));
        return new ScatterResult(landing, target.distance(landing));
    }

    /**
     * Advanced Altitude Bombing Scatter (TO:AR). Like {@link #advanced}, but the initial leg starts on the left or
     * right diagram hexside, the second leg turns the opposite way, and the second leg distance subtracts the dice
     * count.
     *
     * @param target          the hex to scatter from
     * @param facing          the flight-path facing the bomb was dropped along
     * @param marginOfFailure the attack's margin of failure (sign ignored)
     * @param reduction       hexes to subtract from the initial leg; {@code 0} for none
     *
     * @return the scatter outcome
     */
    public static ScatterResult advancedAltitude(Coords target, int facing, int marginOfFailure, int reduction) {
        int dice = diceCount(marginOfFailure);
        boolean startLeft = Compute.d6(1) <= 3;
        int direction = startLeft ? Math.floorMod(facing - 1, 6) : Math.floorMod(facing + 1, 6);
        int firstLeg = Math.max(Compute.d6(dice) - reduction, 0);
        Coords intermediate = target.translated(direction, firstLeg);
        int secondDirection = startLeft ? Math.floorMod(direction + 2, 6) : Math.floorMod(direction - 2, 6);
        int secondLeg = Math.max(Compute.d6(dice) - dice, 0);
        Coords landing = intermediate.translated(secondDirection, secondLeg);
        return new ScatterResult(landing, target.distance(landing));
    }

    /**
     * Single source of truth for straight-line scatter geometry. The distance magnitude is used so a negative value
     * never pushes the shot off the straight-line path; {@code Coords.translated()} truncates toward zero on the
     * diagonal directions (e.g. {@code -5 / 2 == -2} rather than the floored {@code -3}).
     */
    private static ScatterResult inDirection(Coords target, int direction, int distance) {
        Coords landing = target.translated(direction, Math.abs(distance));
        return new ScatterResult(landing, target.distance(landing));
    }
}
