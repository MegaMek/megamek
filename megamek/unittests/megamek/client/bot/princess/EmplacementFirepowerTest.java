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

import static megamek.client.bot.princess.EmplacementFirepower.DEFAULT_GUNNERY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.client.bot.princess.EmplacementFirepower.WeaponReach;
import org.junit.jupiter.api.Test;

/**
 * Verifies what an emplacement's weapons are worth at a range: more the closer the target, nothing past long range,
 * and less inside a minimum range; and how far behind the front line those weapons put it.
 */
class EmplacementFirepowerTest {

    private static final WeaponReach MEDIUM_LASER = new WeaponReach(5, 0, 3, 6, 9, false);
    private static final WeaponReach LARGE_LASER = new WeaponReach(8, 0, 5, 10, 15, false);
    private static final WeaponReach LRM_20 = new WeaponReach(20, 6, 7, 14, 21, true);

    @Test
    void rangeBracketsAddTheirModifiers() {
        assertEquals(0, MEDIUM_LASER.rangeModifier(3));
        assertEquals(2, MEDIUM_LASER.rangeModifier(4));
        assertEquals(4, MEDIUM_LASER.rangeModifier(9));
        assertNull(MEDIUM_LASER.rangeModifier(10), "past long range there is no shot");
    }

    @Test
    void insideMinimumRangeTheShotGetsHarder() {
        assertEquals(1, LRM_20.rangeModifier(6));
        assertEquals(6, LRM_20.rangeModifier(1));
        assertEquals(0, LRM_20.rangeModifier(7));
    }

    @Test
    void aHexIsWorthMoreTheCloserItIs() {
        List<WeaponReach> weapons = List.of(MEDIUM_LASER);
        int atShort = EmplacementFirepower.valueAtRange(weapons, 3, DEFAULT_GUNNERY);
        int atMedium = EmplacementFirepower.valueAtRange(weapons, 6, DEFAULT_GUNNERY);
        int atLong = EmplacementFirepower.valueAtRange(weapons, 9, DEFAULT_GUNNERY);
        assertTrue(atShort > atMedium);
        assertTrue(atMedium > atLong);
        assertTrue(atShort >= (4 * atLong), "short range is worth about four times long range");
        assertEquals(0, EmplacementFirepower.valueAtRange(weapons, 10, DEFAULT_GUNNERY));
    }

    @Test
    void aMissileRackWantsItsTargetsStoodOff() {
        List<WeaponReach> weapons = List.of(LRM_20);
        assertTrue(EmplacementFirepower.valueAtRange(weapons, 7, DEFAULT_GUNNERY)
              > EmplacementFirepower.valueAtRange(weapons, 2, DEFAULT_GUNNERY));
    }

    @Test
    void aBetterGunnerMakesTheSameGroundWorthMore() {
        List<WeaponReach> weapons = List.of(MEDIUM_LASER);
        assertTrue(EmplacementFirepower.valueAtRange(weapons, 6, 2)
              > EmplacementFirepower.valueAtRange(weapons, 6, 5));
    }

    @Test
    void withNoWeaponsEveryHexInViewCountsTheSame() {
        assertEquals(EmplacementFirepower.UNARMED_VALUE,
              EmplacementFirepower.valueAtRange(List.of(), 2, DEFAULT_GUNNERY));
        assertEquals(EmplacementFirepower.UNARMED_VALUE,
              EmplacementFirepower.valueAtRange(List.of(), 9, DEFAULT_GUNNERY));
    }

    @Test
    void shortRangeGunsBelongInTheLineAndLongRangeGunsBehindIt() {
        int mediumLasers = EmplacementFirepower.standoff(List.of(MEDIUM_LASER, MEDIUM_LASER), true);
        int largeLasers = EmplacementFirepower.standoff(List.of(LARGE_LASER, LARGE_LASER), true);
        int missiles = EmplacementFirepower.standoff(List.of(LRM_20), true);
        assertEquals(3, mediumLasers);
        assertEquals(5, largeLasers);
        assertEquals(14, missiles, "a rack that can fire indirectly stands back at its medium range");
    }

    @Test
    void withoutTheIndirectFireRuleAMissileRackStandsWhereItCanSee() {
        assertEquals(7, EmplacementFirepower.standoff(List.of(LRM_20), false));
    }

    @Test
    void aMixedBatteryStandsWhereItsHeaviestGunsFightBest() {
        int mixed = EmplacementFirepower.standoff(List.of(LARGE_LASER, MEDIUM_LASER, MEDIUM_LASER), true);
        assertTrue((mixed > 3) && (mixed < 5), "between the medium lasers at 3 and the large laser at 5: " + mixed);
    }

    @Test
    void onlyIndirectWeaponsCountAgainstGroundOutOfSight() {
        assertTrue(EmplacementFirepower.canFireIndirectly(List.of(MEDIUM_LASER, LRM_20)));
        assertFalse(EmplacementFirepower.canFireIndirectly(List.of(MEDIUM_LASER, LARGE_LASER)));
        assertEquals(0, EmplacementFirepower.indirectValueAtRange(List.of(MEDIUM_LASER), 3, DEFAULT_GUNNERY));
        assertTrue(EmplacementFirepower.indirectValueAtRange(List.of(LRM_20), 10, DEFAULT_GUNNERY) > 0);
    }

    @Test
    void anIndirectShotIsWorthLessThanADirectOne() {
        List<WeaponReach> weapons = List.of(LRM_20);
        assertTrue(EmplacementFirepower.indirectValueAtRange(weapons, 10, DEFAULT_GUNNERY)
              < EmplacementFirepower.valueAtRange(weapons, 10, DEFAULT_GUNNERY));
    }

    @Test
    void aNeighbourIsGuardedOutToMediumRange() {
        assertEquals(6, EmplacementFirepower.guardRange(List.of(MEDIUM_LASER)));
        assertEquals(10, EmplacementFirepower.guardRange(List.of(LARGE_LASER)));
    }
}
