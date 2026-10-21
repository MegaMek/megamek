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
package megamek.server.nuclear;

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.equipment.enums.NuclearWeaponType;
import org.junit.jupiter.api.Test;

/**
 * Blast geometry checked against the worked examples and the Standard Nuclear Weaponry Table of IO:AE 3rd printing,
 * pp. 164-165.
 */
class NuclearBlastProfileTest {

    @Test
    void typeIaEightHexesOutTakesSixty() {
        // IO:AE p.165: "a unit standing 8 hexes from the Ground Zero hex of a ground-bursting Type Ia nuclear weapon
        // would suffer 60 points of damage"
        NuclearBlastProfile profile = NuclearBlastProfile.groundBurst(NuclearWeaponType.DAVY_CROCKETT_I);

        assertEquals(100, profile.damageAt(0));
        assertEquals(60, profile.damageAt(8));
    }

    @Test
    void typeIIICraterShallowsOneLevelEveryTwoHexes() {
        // IO:AE p.164: a Type III reduces Ground Zero by 3 levels, 2 levels at 2 hexes, 1 level at 4 hexes, and is no
        // longer cratered at 6 hexes
        NuclearBlastProfile profile = NuclearBlastProfile.groundBurst(NuclearWeaponType.SANTA_ANA);
        int[] expectedDepths = { 3, 3, 2, 2, 1, 1, 0 };

        for (int distance = 0; distance < expectedDepths.length; distance++) {
            assertEquals(expectedDepths[distance], profile.craterDepthAt(distance), "depth at " + distance + " hexes");
        }
        assertEquals(6, profile.craterRadius());
    }

    @Test
    void alamoCraterIsOneLevelForTwoRings() {
        NuclearBlastProfile profile = NuclearBlastProfile.groundBurst(NuclearWeaponType.ALAMO);

        assertEquals(1, profile.craterDepthAt(0));
        assertEquals(1, profile.craterDepthAt(1));
        assertEquals(0, profile.craterDepthAt(2));
        assertEquals(2, profile.craterRadius());
    }

    @Test
    void santaAnaBlastReachesTheNinetyFirstRing() {
        // Blast radius 92, reduction 109: 10,000 - 91 x 109 = 81 damage at 91 hexes; at 92 hexes nothing is left
        int[] damages = NuclearBlastProfile.groundBurst(NuclearWeaponType.SANTA_ANA).blastDamageByDistance();

        assertEquals(92, damages.length);
        assertEquals(10_000, damages[0]);
        assertEquals(81, damages[91]);
    }

    @Test
    void alamoBlastReachesItsFullRadius() {
        // Blast radius 43, reduction 23: 1,000 - 43 x 23 = 11 damage at 43 hexes
        int[] damages = NuclearBlastProfile.groundBurst(NuclearWeaponType.ALAMO).blastDamageByDistance();

        assertEquals(44, damages.length);
        assertEquals(11, damages[43]);
    }

    @Test
    void peacemakerUsesThirdPrintingReduction() {
        // 3rd printing: reduction 504 per hex (HS:3070 used 505), blast radius 198
        NuclearBlastProfile profile = NuclearBlastProfile.groundBurst(NuclearWeaponType.PEACEMAKER);

        assertEquals(99_496, profile.damageAt(1));
        assertEquals(100_000 - (198 * 504), profile.damageAt(198));
        assertEquals(0, profile.damageAt(199));
        assertEquals(397, profile.secondaryRadius());
    }

    @Test
    void blastStopsWhereDamageRunsOut() {
        // Type Ib: 100 - 20 x 5 = 0 at the edge of its 20-hex radius, so the last damaging ring is 19
        int[] damages = NuclearBlastProfile.groundBurst(NuclearWeaponType.DAVY_CROCKETT_M).blastDamageByDistance();

        assertEquals(20, damages.length);
        assertEquals(5, damages[19]);
    }

    @Test
    void customProfileBlastLastsAsLongAsTheDamage() {
        NuclearBlastProfile profile = NuclearBlastProfile.custom(1_000, 23, 86, 1);

        assertEquals(43, profile.blastRadius());
        assertEquals(11, profile.damageAt(43));
        assertEquals(0, profile.damageAt(44));
    }

    @Test
    void customProfileTreatsZeroReductionAsOne() {
        NuclearBlastProfile profile = NuclearBlastProfile.custom(50, 0, 10, 0);

        assertEquals(1, profile.damageReduction());
        assertEquals(50, profile.blastRadius());
    }
}
