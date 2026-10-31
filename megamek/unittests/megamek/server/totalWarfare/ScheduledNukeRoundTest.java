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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.enums.NuclearWeaponType;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.util.SerializationHelper;
import megamek.server.nuclear.NuclearBlastProfile;
import megamek.server.nuclear.ScheduledNuke;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * A nuclear strike can be set for a later round: it waits in the game until the weapons attack phase of that round,
 * and it is saved with the game while it waits.
 */
class ScheduledNukeRoundTest {
    private static final int BOARD_SIZE = 30;
    private static final Coords GROUND_ZERO = new Coords(14, 14);
    private static final int GAME_MASTER = 0;

    private TWGameManager gameManager;
    private Game game;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).send(anyInt(), any(Packet.class));
        game = gameManager.getGame();
        game.addPlayer(GAME_MASTER, new Player(GAME_MASTER, "Game Master"));
        Hex[] hexes = new Hex[BOARD_SIZE * BOARD_SIZE];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex(0);
        }
        game.setBoard(new Board(BOARD_SIZE, BOARD_SIZE, hexes));
    }

    private ScheduledNuke strikeOnRound(int round) {
        return new ScheduledNuke(GROUND_ZERO, NuclearBlastProfile.groundBurst(NuclearWeaponType.DAVY_CROCKETT_M),
              NuclearWeaponType.DAVY_CROCKETT_M, GAME_MASTER, round);
    }

    @Test
    void earliestRoundIsThisRoundUntilTheWeaponsPhaseIsOver() {
        assertEquals(3, ScheduledNuke.earliestDetonationRound(3, GamePhase.MOVEMENT));
        assertEquals(3, ScheduledNuke.earliestDetonationRound(3, GamePhase.FIRING));
        assertEquals(4, ScheduledNuke.earliestDetonationRound(3, GamePhase.PHYSICAL));
        assertEquals(4, ScheduledNuke.earliestDetonationRound(3, GamePhase.END));
        assertEquals(0, ScheduledNuke.earliestDetonationRound(-1, GamePhase.LOUNGE), "the lobby comes before round 0");
    }

    @Test
    void strikeDroppedInRoundZeroMovementGoesOffThatRound() {
        // A scenario with its units deployed plays round 0 in full (HammerGS, 2026-10-30)
        assertEquals(0, ScheduledNuke.earliestDetonationRound(0, GamePhase.MOVEMENT));

        gameManager.addScheduledNuke(strikeOnRound(0));
        game.setRoundCount(0);
        gameManager.resolveScheduledNukes();

        assertTrue(game.getScheduledNukes().isEmpty(), "it goes off in round 0's weapons phase");
    }

    @Test
    void strikeWaitsForItsRoundAndThenDetonates() {
        gameManager.addScheduledNuke(strikeOnRound(3));

        game.setRoundCount(2);
        gameManager.resolveScheduledNukes();
        assertEquals(1, game.getScheduledNukes().size(), "round 2: the strike set for round 3 is still waiting");

        game.setRoundCount(3);
        gameManager.resolveScheduledNukes();
        assertTrue(game.getScheduledNukes().isEmpty(), "round 3: the strike has gone off");
    }

    @Test
    void waitingStrikeIsSavedWithTheGame() {
        ScheduledNuke strike = strikeOnRound(5);
        game.getScheduledNukes().add(strike);

        String savedGame = SerializationHelper.getSaveGameXStream().toXML(game);
        Game loadedGame = (Game) SerializationHelper.getLoadSaveGameXStream().fromXML(savedGame);

        assertEquals(1, loadedGame.getScheduledNukes().size());
        assertEquals(strike, loadedGame.getScheduledNukes().getFirst(), "every part of the strike comes back");
    }

    @Test
    void waitingAirBurstIsSavedWithTheGame() {
        ScheduledNuke strike = new ScheduledNuke(GROUND_ZERO, NuclearBlastProfile.airBurst(NuclearWeaponType.ALAMO),
              NuclearWeaponType.ALAMO, GAME_MASTER, 5);
        game.getScheduledNukes().add(strike);

        String savedGame = SerializationHelper.getSaveGameXStream().toXML(game);
        Game loadedGame = (Game) SerializationHelper.getLoadSaveGameXStream().fromXML(savedGame);

        assertEquals(strike, loadedGame.getScheduledNukes().getFirst(), "it comes back as an air burst");
        assertTrue(loadedGame.getScheduledNukes().getFirst().profile().isAirBurst());
    }
}
