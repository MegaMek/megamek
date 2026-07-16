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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import megamek.common.Configuration;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.loaders.MapSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link LobbyBoardHandler}: building the game board in the lounge on request, refusing requests that
 * must not be honored (wrong phase, surprise boards), and discarding a built board when a change makes it stale.
 */
class LobbyBoardHandlerTest {

    private static final String BOARD_CONTENT = """
          size 2 2
          hex 0101 0 "" ""
          hex 0201 1 "" ""
          hex 0102 2 "" ""
          hex 0202 3 "" ""
          end""";

    private static final String BOARD_NAME = "lobbyHandlerTestBoard";

    @TempDir
    private static Path temporaryBoardsDirectory;

    private static File originalBoardsDirectory;

    private Game game;
    private LobbyBoardHandler lobbyBoardHandler;

    @BeforeAll
    static void createBoardFileAndRedirectBoardsDirectory() throws IOException {
        Files.writeString(temporaryBoardsDirectory.resolve(BOARD_NAME + ".board"), BOARD_CONTENT);
        originalBoardsDirectory = Configuration.boardsDir();
        Configuration.setBoardsDir(temporaryBoardsDirectory.toFile());
    }

    @AfterAll
    static void restoreBoardsDirectory() {
        Configuration.setBoardsDir(originalBoardsDirectory);
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.setPhase(GamePhase.LOUNGE);
        game.setMapSettings(createMapSettingsFor(List.of(BOARD_NAME)));
        lobbyBoardHandler = new LobbyBoardHandler(spyWithoutNetwork());
    }

    private static MapSettings createMapSettingsFor(List<String> selectedBoards) {
        MapSettings mapSettings = MapSettings.getInstance();
        mapSettings.setBoardSize(2, 2);
        mapSettings.setMapSize(selectedBoards.size(), 1);
        mapSettings.setBoardsSelectedVector(selectedBoards);
        return mapSettings;
    }

    @Test
    void generationRequestInLoungeBuildsAndStoresTheBoard() {
        lobbyBoardHandler.handleGenerationRequest(0);

        assertTrue(lobbyBoardHandler.hasBoardFromLounge());
        assertEquals(2, game.getBoard().getWidth());
        assertEquals(2, game.getBoard().getHeight());
        assertEquals(3, game.getBoard().getHex(1, 1).getLevel());
    }

    @Test
    void generationRequestIsIgnoredOutsideTheLounge() {
        game.setPhase(GamePhase.MOVEMENT);

        lobbyBoardHandler.handleGenerationRequest(0);

        assertFalse(lobbyBoardHandler.hasBoardFromLounge());
    }

    @Test
    void generationRequestIsRefusedWhileASurpriseBoardIsSelected() {
        String surpriseSelection = MapSettings.BOARD_SURPRISE + BOARD_NAME + "\n" + BOARD_NAME;
        game.setMapSettings(createMapSettingsFor(List.of(surpriseSelection)));

        lobbyBoardHandler.handleGenerationRequest(0);

        assertFalse(lobbyBoardHandler.hasBoardFromLounge());
        assertEquals(0, game.getBoard().getWidth());
    }

    @Test
    void invalidateDiscardsTheBuiltBoard() {
        lobbyBoardHandler.handleGenerationRequest(0);
        assertTrue(lobbyBoardHandler.hasBoardFromLounge());

        lobbyBoardHandler.invalidate("map settings changed");

        assertFalse(lobbyBoardHandler.hasBoardFromLounge());
        assertEquals(0, game.getBoard().getWidth());
    }

    @Test
    void invalidateWithoutABuiltBoardDoesNothing() {
        lobbyBoardHandler.invalidate("map settings changed");

        assertFalse(lobbyBoardHandler.hasBoardFromLounge());
    }

    @Test
    void invalidateOutsideTheLoungeLeavesTheGameBoardAlone() {
        lobbyBoardHandler.handleGenerationRequest(0);
        game.setPhase(GamePhase.MOVEMENT);

        lobbyBoardHandler.invalidate("planetary conditions changed");

        // The live game board must never be clobbered after the lounge
        assertEquals(2, game.getBoard().getWidth());
    }

    @Test
    void restoreFromGameKeepsALoungeBuiltBoardAfterALoadedSave() {
        lobbyBoardHandler.handleGenerationRequest(0);

        LobbyBoardHandler reloadedHandler = new LobbyBoardHandler(spyWithoutNetwork());
        reloadedHandler.restoreFromGame(game);

        assertTrue(reloadedHandler.hasBoardFromLounge());
    }

    @Test
    void restoreFromGameIgnoresTheBoardOfARunningGame() {
        lobbyBoardHandler.handleGenerationRequest(0);
        game.setPhase(GamePhase.MOVEMENT);

        LobbyBoardHandler reloadedHandler = new LobbyBoardHandler(spyWithoutNetwork());
        reloadedHandler.restoreFromGame(game);

        assertFalse(reloadedHandler.hasBoardFromLounge());
    }

    /** @return a game manager attached to the test game whose network layer is detached (no live server exists) */
    private TWGameManager spyWithoutNetwork() {
        TWGameManager gameManager = spy(new TWGameManager());
        doNothing().when(gameManager).send(any());
        doNothing().when(gameManager).send(anyInt(), any());
        doNothing().when(gameManager).sendServerChat(anyString());
        doNothing().when(gameManager).sendServerChat(anyInt(), anyString());
        gameManager.setGame(game);
        return gameManager;
    }
}
