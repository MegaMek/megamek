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
package megamek.client.bot.caspar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import megamek.client.bot.princess.AerospaceGroundOrder;

/**
 * The runway-versus-vertical takeoff decision: a clear runway wins outright (nothing to roll), a
 * vertical liftoff is the boxed-in answer above the stunt floor, and below both the fighter stays
 * a ground turret. The crashed-Hellcat game (2026-08-15) is the calibration: at 2 SI its vertical
 * odds sat under the floor, and staying down WAS the right answer.
 */
class CasparTakeoffTest {

    @Test
    void aClearRunwayBeatsEveryVerticalRoll() {
        assertEquals(Caspar.TakeoffMode.RUNWAY,
              Caspar.chooseTakeoffMode(true, true, 0.95),
              "no roll beats even excellent odds");
        assertEquals(Caspar.TakeoffMode.RUNWAY,
              Caspar.chooseTakeoffMode(true, false, 0.0),
              "a clear strip needs no vertical capability at all");
    }

    @Test
    void aBoxedInFighterLiftsVerticallyAboveTheFloor() {
        assertEquals(Caspar.TakeoffMode.VERTICAL,
              Caspar.chooseTakeoffMode(false, true, 0.58),
              "a healthy fighter's ~58% vertical roll clears the floor");
        assertEquals(Caspar.TakeoffMode.VERTICAL,
              Caspar.chooseTakeoffMode(false, true, Caspar.TAKEOFF_ROLL_FLOOR),
              "the floor itself is enough - even odds beat a hopeless siege");
    }

    /**
     * The mission-value gate (SC/DS design): a DropShip or small craft on the ground may be doing
     * its job, so only a standing LIFT_OFF order moves it; fighters need no orders at all. The
     * landing half mirrors it: LAND moves ships down, never fighters.
     */
    @Test
    void dropShipsMoveBetweenDomainsOnlyOnOrders() {
        assertEquals(true, Caspar.groundOrderPermitsTakeoff(true,
                    AerospaceGroundOrder.AUTO),
              "a grounded fighter's job is always to get back up - no order needed");
        assertEquals(false, Caspar.groundOrderPermitsTakeoff(false,
                    AerospaceGroundOrder.AUTO),
              "a DropShip under AUTO holds its domain");
        assertEquals(true, Caspar.groundOrderPermitsTakeoff(false,
                    AerospaceGroundOrder.LIFT_OFF),
              "LIFT_OFF is the order that sends the ships up");
        assertEquals(true, Caspar.groundOrderRequestsLanding(false,
                    AerospaceGroundOrder.LAND),
              "LAND is the order that brings the ships down");
        assertEquals(false, Caspar.groundOrderRequestsLanding(true,
                    AerospaceGroundOrder.LAND),
              "fighters never land on orders - their doctrine flies them");
    }

    @Test
    void aCrippledFighterStaysDownRatherThanRollingTheCrashTable() {
        assertEquals(Caspar.TakeoffMode.STAY,
              Caspar.chooseTakeoffMode(false, true, 0.28),
              "below the floor the expected outcome is the crash that created this situation");
        assertEquals(Caspar.TakeoffMode.STAY,
              Caspar.chooseTakeoffMode(false, false, 0.0),
              "no strip and no vertical capability means fight as a turret");
    }
}
