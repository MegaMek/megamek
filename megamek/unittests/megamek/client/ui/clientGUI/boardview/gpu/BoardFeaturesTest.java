/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Hex;
import megamek.common.board.Coords;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Test;

class BoardFeaturesTest {
    @Test
    void snowTerrainAndThemeSelectSnowAssetsWhileJungleUsesPalms() {
        Hex hex = new Hex(0);
        hex.addTerrain(new Terrain(Terrains.JUNGLE, 2));
        hex.addTerrain(new Terrain(Terrains.FOLIAGE_ELEV, 2));
        Coords coords = new Coords(1, 1);
        assertTrue(BoardFeatures.capture(hex, coords, "").stream().allMatch(feature -> feature.asset().startsWith("palm")));
        hex.addTerrain(new Terrain(Terrains.SNOW, 1));
        assertTrue(BoardFeatures.capture(hex, coords, "").stream().allMatch(feature -> feature.asset().endsWith("-snow")));
        hex.removeTerrain(Terrains.SNOW);
        hex.setTheme("snow");
        assertTrue(BoardFeatures.capture(hex, coords, "").stream().allMatch(feature -> feature.asset().endsWith("-snow")));
        hex.setTheme("");
        assertFalse(BoardFeatures.capture(hex, coords, "").stream().anyMatch(feature -> feature.asset().endsWith("-snow")));
    }

    @Test
    void desertAndSandyWoodsUseOnlyPalmsAtEveryDensity() {
        Coords coords = new Coords(3, 2);
        for (int density = 1; density <= 3; density++) {
            Hex hex = new Hex(0);
            hex.addTerrain(new Terrain(Terrains.WOODS, density));
            hex.addTerrain(new Terrain(Terrains.FOLIAGE_ELEV, 2));
            hex.setTheme("Desert");
            var themed = BoardFeatures.capture(hex, coords, "");
            assertFalse(themed.isEmpty());
            assertTrue(themed.stream().allMatch(feature -> feature.asset().equals("palm")
                  || feature.asset().equals("palm-bent")), "Desert woodland must retain palm silhouettes");
            hex.addTerrain(new Terrain(Terrains.PAVEMENT, 1));
            assertEquals(themed, BoardFeatures.capture(hex, coords, ""), "Ground paving must not change the biome's trees");
            hex.removeTerrain(Terrains.PAVEMENT);
            hex.setTheme("");
            hex.addTerrain(new Terrain(Terrains.SAND, 1));
            assertEquals(themed, BoardFeatures.capture(hex, coords, ""), "Sandy woods use the same palm selection");
            hex.addTerrain(new Terrain(Terrains.SNOW, 1));
            assertTrue(BoardFeatures.capture(hex, coords, "").stream().allMatch(feature -> feature.asset().endsWith("-snow")),
                  "Snow retains the existing winter variants");
        }
    }

    @Test
    void woodlandMixesSilhouettesAndRubbleStaysBelowAThirdOfALevel() {
        Hex hex = new Hex(0);
        Coords coords = new Coords(3, 2);
        hex.addTerrain(new Terrain(Terrains.WOODS, 2));
        hex.addTerrain(new Terrain(Terrains.FOLIAGE_ELEV, 2));
        assertTrue(BoardFeatures.capture(hex, coords, "").stream().map(BoardScene.Feature::asset).distinct().count() >= 5);
        hex.removeAllTerrains();
        hex.addTerrain(new Terrain(Terrains.RUBBLE, 4));
        var rubble = BoardFeatures.capture(hex, coords, "");
        assertEquals(8, rubble.size());
        assertTrue(rubble.stream().allMatch(feature -> feature.asset().startsWith("rock-")
              && feature.height() > 0 && feature.height() < 1f / 3 && feature.elevation() == 0));
        assertEquals(3, rubble.stream().map(BoardScene.Feature::asset).distinct().count());
        assertEquals(rubble, BoardFeatures.capture(hex, coords, ""));
        hex.setTheme("snow");
        assertTrue(BoardFeatures.capture(hex, coords, "").stream().allMatch(feature -> feature.asset().endsWith("-snow")));
    }

    @Test
    void coexistingFeaturesRetainTheirOwnHeightsAndBridgeExits() {
        Hex hex = new Hex(0);
        hex.addTerrain(new Terrain(Terrains.BUILDING, 3, true, 9));
        hex.addTerrain(new Terrain(Terrains.BLDG_ELEV, 4));
        hex.addTerrain(new Terrain(Terrains.BRIDGE, 2, true, 18));
        hex.addTerrain(new Terrain(Terrains.BRIDGE_ELEV, 2));
        hex.addTerrain(new Terrain(Terrains.INDUSTRIAL, 3));
        String selectedRoof = "buildings/saxarba/building_hard/building_hard_09";
        var features = BoardFeatures.capture(hex, new Coords(2, 2), selectedRoof);
        assertTrue(features.stream().anyMatch(feature -> feature.asset().equals(selectedRoof) && feature.height() == 4));
        assertEquals(2, features.stream().filter(feature -> feature.asset().equals("bridge") && feature.elevation() == 2).count());
        assertTrue(features.stream().anyMatch(feature -> feature.asset().equals("industrial") && feature.height() == 3));
        assertEquals(features, BoardFeatures.capture(hex, new Coords(2, 2), selectedRoof), "Placement must remain stable across snapshots");
        assertFalse(BoardFeatures.capture(hex, new Coords(2, 2), "").stream()
              .anyMatch(feature -> feature.asset().startsWith("building")), "Blank tileset artwork must stay blank");
    }

    @Test
    void surfaceMaterialsAndCropsFollowTheHex() {
        Hex hex = new Hex(0);
        hex.addTerrain(new Terrain(Terrains.SAND, 1));
        assertEquals(BoardScene.Surface.SAND, BoardFeatures.surface(hex));
        hex.addTerrain(new Terrain(Terrains.PAVEMENT, 1));
        assertEquals(BoardScene.Surface.CONCRETE, BoardFeatures.surface(hex));
        hex.removeAllTerrains();
        hex.addTerrain(new Terrain(Terrains.ROUGH, 1));
        assertEquals(BoardScene.Surface.ROCK, BoardFeatures.surface(hex));
        hex.removeAllTerrains();
        hex.addTerrain(new Terrain(Terrains.FIELDS, 1));
        assertTrue(BoardFeatures.capture(hex, new Coords(0, 0), "").stream()
              .anyMatch(feature -> feature.asset().equals("field") && feature.height() == 1));
    }
}
