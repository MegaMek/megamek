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
package megamek.client.ui.dialogs.BotCommands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import megamek.common.OffBoardDirection;
import megamek.common.game.Game;
import megamek.common.orders.FormationShape;
import megamek.common.orders.LanceRole;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

/**
 * Tests the commands the lobby's Bot formation menu sends, for one lance and for every lance under a parent force
 * (QA playtest, #9526).
 */
class BotFormationsMenuBuilderTest {

    private static Entity unit(Game game, int unitId) {
        Entity unit = new BipedMek();
        unit.setId(unitId);
        game.addEntity(unit);
        return unit;
    }

    @Test
    void aParentForceGivesEachLanceTheShapeOnItsOwnLeaderAndSkipsALoneUnit() {
        Game game = new Game();
        for (int unitId : List.of(1, 2, 5, 6, 9)) {
            unit(game, unitId);
        }
        // the second lance is a convoy: it keeps a convoy's Run pace and Push through in its new shape
        game.getEntity(5).setLanceRole(LanceRole.convoy(OffBoardDirection.NORTH));
        game.getEntity(6).setLanceRole(LanceRole.convoy(OffBoardDirection.NORTH));
        List<List<Integer>> lances = BotFormationsMenuBuilder.formableLances(List.of(List.of(1, 2), List.of(5, 6),
              List.of(9)));

        assertEquals(List.of(List.of(1, 2), List.of(5, 6)), lances);
        assertEquals(List.of(
              "/unitOrder 1 FORMATION shape=LINE leader=1 spacing=1 slot=0 together=true",
              "/unitOrder 2 FORMATION shape=LINE leader=1 spacing=1 slot=1 together=true",
              "/unitOrder 5 FORMATION shape=LINE leader=5 spacing=1 slot=0 pace=RUN contact=HOLD together=true",
              "/unitOrder 6 FORMATION shape=LINE leader=5 spacing=1 slot=1 pace=RUN contact=HOLD together=true"),
              BotFormationsMenuBuilder.formationCommands(game, lances, FormationShape.LINE));
    }

    @Test
    void formationOffTakesEveryLanceUnderTheParentOutOfFormation() {
        Game game = new Game();
        for (int unitId : List.of(1, 2, 5, 6)) {
            unit(game, unitId);
        }

        assertEquals(List.of("/unitOrder 1 FORMATION_OFF", "/unitOrder 2 FORMATION_OFF", "/unitOrder 5 FORMATION_OFF",
                    "/unitOrder 6 FORMATION_OFF"),
              BotFormationsMenuBuilder.formationCommands(game, List.of(List.of(1, 2), List.of(5, 6)), null));
    }
}
