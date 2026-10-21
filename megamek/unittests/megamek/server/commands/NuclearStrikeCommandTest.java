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
package megamek.server.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.enums.NuclearWeaponType;
import megamek.common.game.Game;
import megamek.server.Server;
import megamek.server.nuclear.NuclearBlastProfile;
import megamek.server.nuclear.ScheduledNuke;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * The GM nuke commands take board numbers (hex 1012 is x 10, y 12) and must schedule the nuke on that hex.
 */
class NuclearStrikeCommandTest {
    private static final int GAME_MASTER_CONNECTION = 1;

    private Server server;
    private TWGameManager gameManager;

    @BeforeEach
    void beforeEach() {
        Player gameMaster = new Player(GAME_MASTER_CONNECTION, "Game Master");
        gameMaster.setGameMaster(true);

        Game game = new Game();
        game.addPlayer(GAME_MASTER_CONNECTION, gameMaster);
        game.setBoard(new Board(16, 17));
        game.getOptions().getOption("allow_nukes").setValue(true);
        game.getOptions().getOption("really_allow_nukes").setValue(true);

        gameManager = Mockito.mock(TWGameManager.class);
        Mockito.when(gameManager.getGame()).thenReturn(game);
        server = Mockito.mock(Server.class);
        Mockito.when(server.getGameManager()).thenReturn(gameManager);
        Mockito.when(server.getGame()).thenReturn(game);
    }

    private ScheduledNuke scheduledNuke() {
        ArgumentCaptor<ScheduledNuke> captor = ArgumentCaptor.forClass(ScheduledNuke.class);
        Mockito.verify(gameManager).addScheduledNuke(captor.capture());
        return captor.getValue();
    }

    @Test
    void nuclearStrikeLandsOnTheNamedHex() {
        new NuclearStrikeCommand(server, gameManager).run(GAME_MASTER_CONNECTION,
              new String[] { "ns", "10", "12", "SANTA_ANA" });

        ScheduledNuke nuke = scheduledNuke();
        assertEquals(new Coords(9, 11), nuke.groundZero(), "hex 1012");
        assertEquals(NuclearBlastProfile.groundBurst(NuclearWeaponType.SANTA_ANA), nuke.profile());
    }

    @Test
    void nukeCommandLandsOnTheNamedHex() {
        new NukeCommand(server, gameManager).run(GAME_MASTER_CONNECTION, new String[] { "nuke", "10", "12", "2" });

        ScheduledNuke nuke = scheduledNuke();
        assertEquals(new Coords(9, 11), nuke.groundZero(), "hex 1012");
        assertEquals(NuclearBlastProfile.groundBurst(NuclearWeaponType.ALAMO), nuke.profile(), "type 2 is the Alamo");
    }

    @Test
    void customNuclearStrikeLandsOnTheNamedHex() {
        new NuclearStrikeCustomCommand(server, gameManager).run(GAME_MASTER_CONNECTION,
              new String[] { "nsc", "10", "12", "1000", "23", "85", "1" });

        ScheduledNuke nuke = scheduledNuke();
        assertEquals(new Coords(9, 11), nuke.groundZero(), "hex 1012");
        assertEquals(NuclearBlastProfile.custom(1000, 23, 85, 1), nuke.profile());
    }
}
