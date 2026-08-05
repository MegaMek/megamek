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
package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Where a force's formation is and how wide it may be: the shape arithmetic both halves of the Mutual Support
 * doctrine measure against.
 *
 * <p>{@link MutualSupportDeployment} uses it to order candidate hexes as a force forms up, and
 * {@link MutualSupportPathRanker} uses it to judge whether a path leaves the force behind. Both must get the same
 * answer to the same question - two definitions would let a force deploy into a formation its own movement code
 * then judges to be broken - so the arithmetic lives here rather than in either of them.</p>
 *
 * @see SupportEnvelope how far one unit can usefully reach, which is what sizes the formation
 */
public final class FormationGeometry {
    private final static MMLogger logger = MMLogger.create(FormationGeometry.class);

    /**
     * Closest a unit will willingly be to a friend, in hexes.
     *
     * <p>Dispersion insurance. A force packed hex-to-hex fits inside a single artillery or bombing template and gets
     * in its own way moving off the start line, so units keep a hex of clear ground between them even though cohesion
     * would happily stack them.</p>
     */
    static final int MINIMUM_SPACING_HEXES = 2;

    private FormationGeometry() {
    }

    /**
     * How far from its centre of mass a force may spread, in hexes.
     *
     * <p>One figure for the whole force, so the formation is a single shape rather than a set of nested per-unit
     * discs. It is sized from the force's own guns: the mean {@link SupportEnvelope#effectiveRange()} of every unit
     * that has any, <b>halved</b>, so that at the default setting the formation's <em>diameter</em> comes out at the
     * average effective range. That is mutual support in the literal sense - any two units in the formation are
     * within supporting range of <em>each other</em>, not merely of the centre. Taking the radius as the full average
     * instead would let a company spread to twice its own supporting range, which is the dispersion this rule exists
     * to fix.</p>
     *
     * <p>The multiplier is the player's mutual support setting, and it divides rather than multiplies: asking for
     * more mutual support pulls the formation in. At the lowest setting the radius grows past any real deployment
     * zone and the rule stops constraining anything, which reproduces stock scattered deployment.</p>
     *
     * @param force                   the units forming up, deployed or not; a whole command, so the figure is stable
     *                                across the deployment phase instead of drifting as units land
     * @param mutualSupportMultiplier the player's mutual support setting; higher means a tighter formation
     *
     * @return the formation radius in hexes, never below {@link #MINIMUM_SPACING_HEXES}
     */
    public static int formationRadius(List<Entity> force, double mutualSupportMultiplier) {
        List<Integer> effectiveRanges = new ArrayList<>(force.size());
        for (Entity unit : force) {
            int effectiveRange = SupportEnvelope.of(unit).effectiveRange();
            if (effectiveRange > 0) {
                effectiveRanges.add(effectiveRange);
            }
        }
        return formationRadiusFor(effectiveRanges, mutualSupportMultiplier);
    }

    /**
     * The formation radius arithmetic, split out from reading the force so it can be exercised directly.
     *
     * @param effectiveRanges         the supporting range of every armed unit in the force
     * @param mutualSupportMultiplier the player's mutual support setting
     *
     * @return the formation radius in hexes
     */
    static int formationRadiusFor(List<Integer> effectiveRanges, double mutualSupportMultiplier) {
        if (effectiveRanges.isEmpty() || (mutualSupportMultiplier <= 0)) {
            // Either nothing in the force has a weapon, or the setting is off. Both leave the radius with
            // nothing to size it from, so it collapses to bare spacing and the rule stops constraining.
            logger.debug("[MutualSupport] formation radius [{}]: {} armed unit(s), setting {}",
                  MINIMUM_SPACING_HEXES, effectiveRanges.size(), mutualSupportMultiplier);
            return MINIMUM_SPACING_HEXES;
        }
        long totalEffectiveRange = 0;
        for (int effectiveRange : effectiveRanges) {
            totalEffectiveRange += effectiveRange;
        }
        double averageEffectiveRange = (double) totalEffectiveRange / effectiveRanges.size();
        return Math.max(MINIMUM_SPACING_HEXES,
              (int) Math.round(averageEffectiveRange / 2.0 / mutualSupportMultiplier));
    }

    /**
     * Centre of mass of a set of hexes with per-hex weights, rounded to the nearest hex.
     *
     * <p>Weights exist so a unit's influence on where its force is can fade rather than vanish. A unit that drops out
     * of the formation entirely moves the centre discontinuously, and that jump lands exactly when a force starts
     * taking casualties - measured, it costs the remaining units real mutual support.</p>
     *
     * @param positions the hexes
     * @param weights   one weight per hex, in the same order; zero or negative weights are ignored
     *
     * @return the weighted centre, or {@code null} when no hex carries any weight
     */
    static @Nullable Coords weightedCentroid(List<Coords> positions, List<Double> weights) {
        double totalWeight = 0;
        double totalX = 0;
        double totalY = 0;
        for (int index = 0; index < positions.size(); index++) {
            double weight = weights.get(index);
            if (weight <= 0) {
                continue;
            }
            totalWeight += weight;
            totalX += positions.get(index).getX() * weight;
            totalY += positions.get(index).getY() * weight;
        }
        if (totalWeight <= 0) {
            return null;
        }
        return new Coords(Math.round((float) (totalX / totalWeight)), Math.round((float) (totalY / totalWeight)));
    }

    /**
     * Centre of mass of a set of hexes, rounded to the nearest hex.
     *
     * @param positions the hexes
     *
     * @return their centre, or {@code null} when the list is empty
     */
    static @Nullable Coords centroid(List<Coords> positions) {
        if (positions.isEmpty()) {
            return null;
        }
        long totalX = 0;
        long totalY = 0;
        for (Coords position : positions) {
            totalX += position.getX();
            totalY += position.getY();
        }
        return new Coords(Math.round((float) totalX / positions.size()),
              Math.round((float) totalY / positions.size()));
    }
}
