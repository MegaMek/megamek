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
package megamek.client.ratgenerator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;

/**
 * Covers which designs may take over a ship's job when the player rerolls it.
 */
class ShipRerollerTest {

    /** An Overlord (2762): 36 Mek bays, 6 fighter bays. */
    private static final Map<Integer, Integer> OVERLORD = Map.of(UnitType.MEK, 36, UnitType.AEROSPACE_FIGHTER, 6);
    /** A Union (2708): 12 Mek bays, 2 fighter bays. */
    private static final Map<Integer, Integer> UNION = Map.of(UnitType.MEK, 12, UnitType.AEROSPACE_FIGHTER, 2);
    /** An Overlord (2817) Command: 32 Mek bays, 6 fighter bays. */
    private static final Map<Integer, Integer> OVERLORD_COMMAND =
          Map.of(UnitType.MEK, 32, UnitType.AEROSPACE_FIGHTER, 6);
    /** A Gazelle (2531): 15 light vehicle bays and no fighters. */
    private static final Map<Integer, Integer> GAZELLE = Map.of(UnitType.VTOL, 15);

    @Test
    void aReplacementCarriesTheSameKindsWithinReason() {
        assertTrue(ShipReroller.isReplacementFor(OVERLORD, OVERLORD_COMMAND, 0, 0),
              "32 Mek bays for 36 is the same job");
        assertFalse(ShipReroller.isReplacementFor(OVERLORD, UNION, 0, 0), "12 Mek bays for 36 is a third of the job");
        assertFalse(ShipReroller.isReplacementFor(UNION, OVERLORD, 0, 0), "36 Mek bays for 12 is three times it");
        assertFalse(ShipReroller.isReplacementFor(OVERLORD, GAZELLE, 0, 0), "A vehicle carrier has no Mek bays");
    }

    @Test
    void fighterBaysDoNotHaveToMatchBecauseTheComplementIsRegenerated() {
        Map<Integer, Integer> overlordWithoutFighterBays = Map.of(UnitType.MEK, 36);

        assertTrue(ShipReroller.isReplacementFor(OVERLORD, overlordWithoutFighterBays, 0, 0));
    }

    @Test
    void aJumpShipKeepsCollarsEnoughForWhatIsDocked() {
        Map<Integer, Integer> noBays = Map.of();

        assertTrue(ShipReroller.isReplacementFor(noBays, noBays, 4, 3), "Four collars for three docked ships");
        assertFalse(ShipReroller.isReplacementFor(noBays, noBays, 2, 3), "Two collars would strand one");
    }
}
