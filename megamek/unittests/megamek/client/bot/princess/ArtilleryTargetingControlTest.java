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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.client.bot.princess.ArtilleryCommandAndControl.ArtilleryOrder;
import megamek.client.bot.princess.ArtilleryCommandAndControl.SpecialAmmo;
import megamek.common.HexTarget;
import megamek.common.board.Coords;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for ordered fire mission targeting (Fixes #8319): hexes ordered via the artillery chat command
 * (barrage/volley/single) must be treated as commands and fired upon even when no enemy units are near them (area
 * denial), instead of being valued by expected damage to nearby units which is zero for an empty hex.
 *
 * @author HammerGS
 */
class ArtilleryTargetingControlTest {

    private static final Coords ORDERED_HEX = new Coords(7, 7);
    private static final Coords OTHER_HEX = new Coords(2, 2);

    private ArtilleryTargetingControl artilleryTargetingControl;
    private ArtilleryCommandAndControl artilleryCommandAndControl;

    @BeforeEach
    void beforeEach() {
        artilleryTargetingControl = new ArtilleryTargetingControl();
        artilleryCommandAndControl = new ArtilleryCommandAndControl();
        artilleryCommandAndControl.addArtilleryTargets(List.of(ORDERED_HEX));
    }

    private Targetable hexTargetAt(Coords coords) {
        return new HexTarget(coords, Targetable.TYPE_HEX_ARTILLERY);
    }

    @Test
    void testBarrageOrderedHexIsFireMissionTarget() {
        artilleryCommandAndControl.setArtilleryOrder(ArtilleryOrder.BARRAGE, SpecialAmmo.STANDARD);

        assertTrue(artilleryTargetingControl.isOrderedFireMissionTarget(artilleryCommandAndControl,
              hexTargetAt(ORDERED_HEX)), "A barrage order must treat the ordered hex as a fire mission target");
    }

    @Test
    void testVolleyOrderedHexIsFireMissionTarget() {
        artilleryCommandAndControl.setArtilleryOrder(ArtilleryOrder.VOLLEY, SpecialAmmo.STANDARD);

        assertTrue(artilleryTargetingControl.isOrderedFireMissionTarget(artilleryCommandAndControl,
              hexTargetAt(ORDERED_HEX)), "A volley order must treat the ordered hex as a fire mission target");
    }

    @Test
    void testSingleOrderedHexIsFireMissionTarget() {
        artilleryCommandAndControl.setArtilleryOrder(ArtilleryOrder.SINGLE, SpecialAmmo.STANDARD);

        assertTrue(artilleryTargetingControl.isOrderedFireMissionTarget(artilleryCommandAndControl,
              hexTargetAt(ORDERED_HEX)), "A single order must treat the ordered hex as a fire mission target");
    }

    @Test
    void testUnorderedHexIsNotFireMissionTarget() {
        artilleryCommandAndControl.setArtilleryOrder(ArtilleryOrder.BARRAGE, SpecialAmmo.STANDARD);

        assertFalse(artilleryTargetingControl.isOrderedFireMissionTarget(artilleryCommandAndControl,
              hexTargetAt(OTHER_HEX)), "Hexes that were not ordered must not be treated as fire mission targets");
    }

    @Test
    void testAutoOrderHexIsNotFireMissionTarget() {
        artilleryCommandAndControl.setArtilleryOrder(ArtilleryOrder.AUTO, SpecialAmmo.STANDARD);

        assertFalse(artilleryTargetingControl.isOrderedFireMissionTarget(artilleryCommandAndControl,
                    hexTargetAt(ORDERED_HEX)),
              "In auto mode target hexes must use the normal expected-damage evaluation");
    }

    @Test
    void testHaltOrderHexIsNotFireMissionTarget() {
        artilleryCommandAndControl.setArtilleryOrder(ArtilleryOrder.HALT, SpecialAmmo.STANDARD);

        assertFalse(artilleryTargetingControl.isOrderedFireMissionTarget(artilleryCommandAndControl,
              hexTargetAt(ORDERED_HEX)), "A halted artillery must not have fire mission targets");
    }
}
