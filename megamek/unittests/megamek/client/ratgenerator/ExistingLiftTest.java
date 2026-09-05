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

import static megamek.testUtilities.MMTestUtilities.getEntityForUnitTesting;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;

/**
 * Covers working out what lift a force already owns, so a later layer of a command starts from the ships the
 * earlier layers brought.
 */
class ExistingLiftTest {

    private static final String DROPSHIP = "Leopard (2537)";
    private static final String WARSHIP = "Aegis Heavy Cruiser (2372)";
    private static final String FIGHTER = "Cheetah F-11";
    private static final String MEK = "Atlas AS7-D";

    @Test
    void aShipOnItsOwnOffersEveryBay() {
        ExistingLift lift = ExistingLift.of(List.of(load(DROPSHIP, true)));

        assertEquals(4, lift.freeBays().get(UnitType.MEK), "A Leopard has four Mek bays");
        assertEquals(2, lift.freeBays().get(UnitType.AEROSPACE_FIGHTER), "and two fighter bays");
        assertEquals(0, lift.freeDockingCollars(), "A DropShip has no collars to offer");
    }

    @Test
    void unitsWithNoShipYetTakeTheBaysTheyWillNeed() {
        Entity leopard = load(DROPSHIP, true);
        Entity firstFighter = load(FIGHTER, true);
        Entity secondFighter = load(FIGHTER, true);
        Entity atlas = load(MEK, false);

        ExistingLift lift = ExistingLift.of(List.of(leopard, firstFighter, secondFighter, atlas));

        assertEquals(3, lift.freeBays().get(UnitType.MEK), "The Atlas has one of the four Mek bays spoken for");
        assertFalse(lift.freeBays().containsKey(UnitType.AEROSPACE_FIGHTER), "Both fighter bays are spoken for");
    }

    @Test
    void aUnitAlreadyAboardIsNeitherDemandNorFreeSpace() {
        Game game = new Game();
        game.setPhase(GamePhase.LOUNGE);
        Entity leopard = load(DROPSHIP, true);
        Entity fighter = load(FIGHTER, true);
        leopard.setId(1);
        fighter.setId(2);
        leopard.setGame(game);
        fighter.setGame(game);
        leopard.load(fighter, false);
        fighter.setTransportId(leopard.getId());

        ExistingLift lift = ExistingLift.of(List.of(leopard, fighter));

        assertEquals(1, lift.freeBays().get(UnitType.AEROSPACE_FIGHTER), "One fighter bay is taken, one is free");
    }

    @Test
    void anUndockedDropShipTakesACollar() {
        Entity aegis = load(WARSHIP, true);
        Entity leopard = load(DROPSHIP, true);

        ExistingLift lift = ExistingLift.of(List.of(aegis, leopard));

        assertEquals(3, lift.freeDockingCollars(), "The Aegis has four collars and the Leopard wants one");
    }

    @Test
    void liftsAddUp() {
        ExistingLift inModel = new ExistingLift(Map.of(UnitType.MEK, 12), 1);
        ExistingLift inGame = new ExistingLift(Map.of(UnitType.MEK, 4, UnitType.TANK, 6), 2);

        ExistingLift total = inModel.plus(inGame);

        assertEquals(16, total.freeBays().get(UnitType.MEK));
        assertEquals(6, total.freeBays().get(UnitType.TANK));
        assertEquals(3, total.freeDockingCollars());
    }

    @Test
    void nothingOwnedIsEmpty() {
        assertTrue(ExistingLift.NONE.isEmpty());
        assertTrue(ExistingLift.of((ForceDescriptor) null).isEmpty());
        assertFalse(new ExistingLift(Map.of(), 1).isEmpty());
    }

    private static Entity load(String unitName, boolean isBlk) {
        Entity entity = getEntityForUnitTesting(unitName, isBlk);
        assertNotNull(entity, unitName + " not found in the test data");
        return entity;
    }
}
