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

import megamek.common.analysis.DamageProfile;
import megamek.common.units.Entity;

/**
 * How far a unit can usefully reach, in hexes: the single definition of "supporting range" for the Mutual Support
 * doctrine.
 *
 * <p>Both halves of the doctrine ask the same question of a unit and must get the same answer. Deployment uses it to
 * decide how far apart a force may form up ({@link MutualSupportDeployment}); movement uses it to decide when a unit
 * has drifted out of support and when it is covering a friend ({@link MutualSupportPathRanker}). Two definitions would
 * let a force deploy into a formation its own movement code then judges to be broken.</p>
 *
 * <p>Both figures come from {@link DamageProfile}, the same expected-damage model behind the lobby's unit analysis, so
 * a unit's supporting range is whatever its actual weapons say it is rather than a guess from its class or role.</p>
 *
 * @param peakRange      the range at which the unit's expected damage peaks - its optimum weapon range
 * @param effectiveRange the furthest range at which it still lands at least half its peak damage
 */
public record SupportEnvelope(int peakRange, int effectiveRange) {

    /** A unit with nothing to shoot supports nobody at any distance. */
    public static final SupportEnvelope WEAPONLESS = new SupportEnvelope(0, 0);

    /** Fraction of peak expected damage a unit must still land for a range to count as supporting. */
    private static final double SUPPORT_THRESHOLD_FRACTION = 0.5;

    /** Gunnery assumed when a unit has no crew to ask, matching the game's default skill. */
    private static final int DEFAULT_GUNNERY = 4;

    /**
     * Computes a unit's supporting range from its weapons and its own pilot's gunnery.
     *
     * <p>Callers evaluating many units in a turn should cache the result; this walks the unit's whole weapon list.</p>
     *
     * @param entity the unit to measure
     *
     * @return its engagement envelope, or {@link #WEAPONLESS} if it has no weapons
     */
    public static SupportEnvelope of(Entity entity) {
        int gunnery = (entity.getCrew() != null) ? entity.getCrew().getGunnery() : DEFAULT_GUNNERY;
        DamageProfile profile = DamageProfile.of(entity, false, gunnery);
        if (!profile.hasWeapons()) {
            return WEAPONLESS;
        }
        int peakRange = profile.peakExpectedRange();
        double supportThreshold = profile.peakExpectedDamage() * SUPPORT_THRESHOLD_FRACTION;
        int effectiveRange = peakRange;
        for (int range = profile.maxRange(); range > peakRange; range--) {
            if (profile.expectedDamage(range) >= supportThreshold) {
                effectiveRange = range;
                break;
            }
        }
        return new SupportEnvelope(peakRange, effectiveRange);
    }
}
