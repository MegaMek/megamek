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
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import java.util.Vector;

import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * The Game Master's Start a Fire and Start a Firestorm tools refused with "Current game options do not allow fires"
 * whenever the TacOps start-fire option was off. That option is about attacks starting fires, not about a Game Master
 * placing one.
 */
class GameMasterIgniteTest {

    private static final Coords HEX = new Coords(0, 0);

    private TWGameManager gameManager;
    private Game game;

    @BeforeEach
    void setUp() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).sendChangedHex(any(Coords.class), anyInt());
        game = gameManager.getGame();
        game.setBoard(new Board(1, 1, new Hex()));
        // every test here is about the option being off; set it rather than lean on today's default
        game.getOptions().getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_START_FIRE).setValue(false);
    }

    private boolean hexIsOnFire() {
        return game.getBoard().getHex(HEX).containsTerrain(Terrains.FIRE);
    }

    @Test
    void theGameMasterCanStartAFireWithTheFireOptionOff() {
        gameManager.igniteByGameMaster(HEX, Terrains.FIRE_LVL_NORMAL, new Vector<Report>());

        assertTrue(hexIsOnFire(), "a fire the Game Master places must be placed, whatever the TacOps option says");
    }

    @Test
    void anOrdinaryIgnitionStillFollowsTheFireOption() {
        // the positive case above would also pass if every ignite ignored the option; this keeps that honest
        gameManager.ignite(HEX, Terrains.FIRE_LVL_NORMAL, new Vector<Report>());

        assertFalse(hexIsOnFire(), "attacks must still start no fires while the TacOps option is off");
    }

    @Test
    void theGameMasterStillCannotBurnAVacuum() {
        game.getPlanetaryConditions().setAtmosphere(Atmosphere.VACUUM);

        gameManager.igniteByGameMaster(HEX, Terrains.FIRE_LVL_NORMAL, new Vector<Report>());

        assertFalse(hexIsOnFire(), "the planetary conditions make fire impossible, which no order can change");
    }
}
