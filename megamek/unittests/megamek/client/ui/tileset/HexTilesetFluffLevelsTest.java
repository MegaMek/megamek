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
package megamek.client.ui.tileset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import megamek.common.Configuration;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.game.IGame;
import megamek.common.enums.BasementType;
import megamek.common.units.IBuilding;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies which fluff images a tileset reports for a given building.
 *
 * <p>A fluff image is chosen by number, and the numbers are not a range: the tileset defines particular ones and ties
 * each to a particular building, so a light building may have four images where a hardened one has fifty. Anything
 * offering a gamemaster the whole range offers mostly numbers that draw nothing, which is what these levels exist to
 * narrow down.</p>
 */
class HexTilesetFluffLevelsTest {

    private static HexTileset tileset;
    private static File originalDataDirectory;

    @TempDir
    private static Path dataDirectory;

    @BeforeAll
    static void beforeAll() throws IOException {
        originalDataDirectory = Configuration.dataDir();
        Path hexes = dataDirectory.resolve("images").resolve("hexes");
        Files.createDirectories(hexes);
        Files.writeString(hexes.resolve("fluff.tileset"), """
              super * "building:1;bldg_elev:*;bldg_cf:*;bldg_fluff:1" "" "light_one.png"
              super * "building:1;bldg_elev:*;bldg_cf:*;bldg_fluff:2" "" "light_two.png"
              super * "building:4;bldg_elev:*;bldg_cf:*;bldg_fluff:1" "" "hardened_one.png"
              super * "building:4;bldg_elev:*;bldg_cf:*;bldg_fluff:31" "" "hardened_thirty_one.png"
              super * "building:4;bldg_elev:*;bldg_cf:*;bldg_class:3;bldg_fluff:44" "" "emplacement.png"
              super * "building:4;bldg_elev:*;bldg_cf:*;bldg_basement_type:*;bldg_fluff:77" "" "cellar.png"
              super * "bldg_fluff:*" "" "blank.png"
              base * "" "" "grass.png"
              """);
        Configuration.setDataDir(dataDirectory.toFile());

        IGame game = mock(IGame.class);
        when(game.getBoard()).thenReturn(new Board(1, 1));
        tileset = new HexTileset(game);
        tileset.loadFromFile("fluff.tileset");
    }

    @AfterAll
    static void afterAll() {
        Configuration.setDataDir(originalDataDirectory);
    }

    /** @return a hex holding the described building, the way the board would hold it */
    private static Hex building(int buildingType, int buildingClass) {
        Hex hex = new Hex();
        hex.addTerrain(new Terrain(Terrains.BUILDING, buildingType));
        hex.addTerrain(new Terrain(Terrains.BLDG_ELEV, 1));
        hex.addTerrain(new Terrain(Terrains.BLDG_CF, 40));
        hex.addTerrain(new Terrain(Terrains.BLDG_CLASS, buildingClass));
        return hex;
    }

    @Test
    void aLightBuildingIsOfferedOnlyItsOwnImages() {
        List<Integer> levels = tileset.definedFluffLevels(building(1, IBuilding.STANDARD), Terrains.BLDG_FLUFF);
        assertEquals(List.of(1, 2), levels);
    }

    @Test
    void aHardenedBuildingIsOfferedADifferentSetFromALightOne() {
        List<Integer> levels = tileset.definedFluffLevels(building(4, IBuilding.STANDARD), Terrains.BLDG_FLUFF);
        assertEquals(List.of(1, 31), levels);
    }

    @Test
    void anImageTiedToABuildingClassIsOfferedOnlyToThatClass() {
        Hex emplacement = building(4, IBuilding.GUN_EMPLACEMENT);
        assertTrue(tileset.definedFluffLevels(emplacement, Terrains.BLDG_FLUFF).contains(44));
        assertFalse(tileset.definedFluffLevels(building(4, IBuilding.STANDARD), Terrains.BLDG_FLUFF).contains(44));
    }

    @Test
    void anImageIsNotOfferedWhenTheBuildingLacksATerrainTheEntryAsksFor() {
        // the "cellar" entry asks for a basement at any depth, and a hex with no basement terrain at all is not a hex
        // that entry was drawn for - the picture would not appear, so the number must not be offered
        Hex noBasement = building(4, IBuilding.STANDARD);
        assertFalse(tileset.definedFluffLevels(noBasement, Terrains.BLDG_FLUFF).contains(77));

        Hex withBasement = building(4, IBuilding.STANDARD);
        withBasement.addTerrain(new Terrain(Terrains.BLDG_BASEMENT_TYPE, BasementType.TWO_DEEP_HEAD.ordinal()));
        assertTrue(tileset.definedFluffLevels(withBasement, Terrains.BLDG_FLUFF).contains(77));
    }

    @Test
    void theCatchAllEntryOffersNothingToChoose() {
        // "bldg_fluff:*" matches every level and names no particular picture, so it must not appear as a choice
        List<Integer> levels = tileset.definedFluffLevels(building(2, IBuilding.STANDARD), Terrains.BLDG_FLUFF);
        assertTrue(levels.isEmpty());
    }
}
