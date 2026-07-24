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

package megamek.common.board.postprocess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import megamek.common.Configuration;
import megamek.common.board.Board;
import megamek.common.loaders.MapSettings;
import megamek.common.options.GameOptions;
import megamek.common.planetaryConditions.PlanetaryConditions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Characterization tests for {@link TWBoardTransformer}. These pin the behavior of the board assembly pipeline
 * (loading fixed boards, rotation requests, sheet combination, surprise board resolution) so that consolidating
 * the duplicate board-build code paths in the server does not change behavior.
 */
class TWBoardTransformerTest {

    /**
     * A 2x2 board whose four hexes carry distinct levels so that position, combination and rotation can be
     * asserted unambiguously. Levels by (x, y): (0,0)=0, (1,0)=1, (0,1)=2, (1,1)=3.
     */
    private static final String FIRST_BOARD_CONTENT = """
          size 2 2
          hex 0101 0 "" ""
          hex 0201 1 "" ""
          hex 0102 2 "" ""
          hex 0202 3 "" ""
          end""";

    /** A second 2x2 board with levels 4-7, distinguishable from the first when sheets are combined. */
    private static final String SECOND_BOARD_CONTENT = """
          size 2 2
          hex 0101 4 "" ""
          hex 0201 5 "" ""
          hex 0102 6 "" ""
          hex 0202 7 "" ""
          end""";

    private static final String FIRST_BOARD_NAME = "transformerTestBoardOne";
    private static final String SECOND_BOARD_NAME = "transformerTestBoardTwo";

    @TempDir
    private static Path temporaryBoardsDirectory;

    private static File originalBoardsDirectory;

    @BeforeAll
    static void createBoardFilesAndRedirectBoardsDirectory() throws IOException {
        Files.writeString(temporaryBoardsDirectory.resolve(FIRST_BOARD_NAME + ".board"), FIRST_BOARD_CONTENT);
        Files.writeString(temporaryBoardsDirectory.resolve(SECOND_BOARD_NAME + ".board"), SECOND_BOARD_CONTENT);
        originalBoardsDirectory = Configuration.boardsDir();
        Configuration.setBoardsDir(temporaryBoardsDirectory.toFile());
    }

    @AfterAll
    static void restoreBoardsDirectory() {
        Configuration.setBoardsDir(originalBoardsDirectory);
    }

    private static MapSettings createMapSettingsFor(List<String> selectedBoards) {
        MapSettings mapSettings = MapSettings.getInstance();
        mapSettings.setBoardSize(2, 2);
        mapSettings.setMapSize(selectedBoards.size(), 1);
        mapSettings.setBoardsSelectedVector(selectedBoards);
        return mapSettings;
    }

    @Test
    void loadsFixedBoardFromMapSettings() {
        MapSettings mapSettings = createMapSettingsFor(List.of(FIRST_BOARD_NAME));

        Board board = TWBoardTransformer.instantiateBoard(mapSettings, new PlanetaryConditions(), new GameOptions());

        assertEquals(2, board.getWidth());
        assertEquals(2, board.getHeight());
        assertEquals(0, board.getHex(0, 0).getLevel());
        assertEquals(1, board.getHex(1, 0).getLevel());
        assertEquals(2, board.getHex(0, 1).getLevel());
        assertEquals(3, board.getHex(1, 1).getLevel());
    }

    @Test
    void appliesRequestedRotationForEvenWidthBoards() {
        MapSettings mapSettings = createMapSettingsFor(List.of(Board.BOARD_REQUEST_ROTATION + FIRST_BOARD_NAME));

        Board board = TWBoardTransformer.instantiateBoard(mapSettings, new PlanetaryConditions(), new GameOptions());

        // A rotation request flips the board 180 degrees: each hex swaps with its diagonal opposite
        assertEquals(3, board.getHex(0, 0).getLevel());
        assertEquals(2, board.getHex(1, 0).getLevel());
        assertEquals(1, board.getHex(0, 1).getLevel());
        assertEquals(0, board.getHex(1, 1).getLevel());
    }

    @Test
    void combinesMultipleBoardSheetsLeftToRight() {
        MapSettings mapSettings = createMapSettingsFor(List.of(FIRST_BOARD_NAME, SECOND_BOARD_NAME));

        Board board = TWBoardTransformer.instantiateBoard(mapSettings, new PlanetaryConditions(), new GameOptions());

        assertEquals(4, board.getWidth());
        assertEquals(2, board.getHeight());
        // Left sheet is the first selected board, right sheet the second
        assertEquals(0, board.getHex(0, 0).getLevel());
        assertEquals(1, board.getHex(1, 0).getLevel());
        assertEquals(4, board.getHex(2, 0).getLevel());
        assertEquals(5, board.getHex(3, 0).getLevel());
    }

    @Test
    void generatedBoardMatchesRequestedDimensions() {
        MapSettings mapSettings = MapSettings.getInstance();
        mapSettings.setBoardSize(16, 17);
        mapSettings.setMapSize(1, 1);
        mapSettings.setBoardsSelectedVector(List.of(MapSettings.BOARD_GENERATED));

        Board board = TWBoardTransformer.instantiateBoard(mapSettings, new PlanetaryConditions(), new GameOptions());

        assertEquals(16, board.getWidth());
        assertEquals(17, board.getHeight());
    }

    @Test
    void instantiateBoardDoesNotMutateSourceMapSettings() {
        String surpriseSelection = MapSettings.BOARD_SURPRISE + FIRST_BOARD_NAME + "\n" + SECOND_BOARD_NAME;
        MapSettings mapSettings = createMapSettingsFor(List.of(surpriseSelection));

        TWBoardTransformer.instantiateBoard(mapSettings, new PlanetaryConditions(), new GameOptions());

        // The copy-first contract: resolving the surprise pick must not leak into the caller's settings
        assertEquals(surpriseSelection, mapSettings.getBoardsSelectedVector().get(0));
    }

    @Test
    void instantiateBoardResolvingSettingsRecordsSurprisePickInSettings() {
        String surpriseSelection = MapSettings.BOARD_SURPRISE + FIRST_BOARD_NAME + "\n" + SECOND_BOARD_NAME;
        MapSettings mapSettings = createMapSettingsFor(List.of(surpriseSelection));

        TWBoardTransformer.instantiateBoardResolvingSettings(mapSettings, new PlanetaryConditions(),
              new GameOptions());

        // The resolve-in-place contract: the server's settings must record which board was actually picked,
        // as the dataset logger and save games read the settings after the board is built
        String resolvedSelection = mapSettings.getBoardsSelectedVector().get(0);
        assertTrue(FIRST_BOARD_NAME.equals(resolvedSelection) || SECOND_BOARD_NAME.equals(resolvedSelection),
              "Surprise selection must be resolved to a concrete board name, but was " + resolvedSelection);
    }

    @Test
    void surprisePickResolvesToOneOfTheOfferedBoards() {
        String surpriseSelection = MapSettings.BOARD_SURPRISE + FIRST_BOARD_NAME + "\n" + SECOND_BOARD_NAME;
        MapSettings mapSettings = createMapSettingsFor(List.of(surpriseSelection));

        Board board = TWBoardTransformer.instantiateBoard(mapSettings, new PlanetaryConditions(), new GameOptions());

        // Whichever board was picked, hex (0,0) carries that board's marker level
        int cornerLevel = board.getHex(0, 0).getLevel();
        assertTrue((cornerLevel == 0) || (cornerLevel == 4),
              "Surprise pick must load one of the two offered boards, but corner level was " + cornerLevel);
    }
}
