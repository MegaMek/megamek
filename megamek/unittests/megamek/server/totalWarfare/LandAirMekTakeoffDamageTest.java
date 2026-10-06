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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.LandAirMek;
import megamek.testUtilities.MMTestUtilities;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A unit taking off with something loaded that same turn takes a critical hit. The old code applied it as an aerospace
 * critical through an {@code (Aero)} cast; a LAM implements the aerospace interface but is a Mek, so the cast threw
 * (found while auditing LAM casts for issue #8027).
 */
class LandAirMekTakeoffDamageTest {

    private TWGameManager gameManager;
    private Game game;

    @BeforeEach
    void setUp() throws IOException {
        gameManager = new TWGameManager();
        game = gameManager.getGame();
        ServerFactory.createServer(gameManager);
        game.addPlayer(0, new Player(0, "Test"));
        game.initializeRulesManager(OptionsConstants.RULES_CORE);

        Board board = new Board(3, 3);
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                board.setHex(x, y, new Hex());
            }
        }
        game.setBoard(board);
    }

    @Test
    void lamTakingOffWithUnsecuredCargoDoesNotThrow() {
        LandAirMek lam = spy(new LandAirMek(LandAirMek.GYRO_STANDARD, LandAirMek.COCKPIT_STANDARD,
              LandAirMek.LAM_STANDARD));
        lam.setOwner(game.getPlayer(0));
        lam.setConversionMode(LandAirMek.CONV_MODE_FIGHTER);
        lam.setPosition(new Coords(1, 1));
        game.addEntity(lam);

        Entity cargo = MMTestUtilities.getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(cargo, "the Atlas test unit should load");
        cargo.setOwner(game.getPlayer(0));
        game.addEntity(cargo);
        cargo.setLoadedThisTurn(true);
        when(lam.getLoadedUnits()).thenReturn(List.of(cargo));

        assertDoesNotThrow(() -> gameManager.checkForTakeoffDamage(lam));
    }
}
