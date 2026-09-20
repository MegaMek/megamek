/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import megamek.common.Hex;
import megamek.common.board.Coords;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Test;

class BoardFeaturesTest {
    @Test
    void treeCountsFollowAuthoritativeCoverReductionUntilTheHexIsClear() {
        Coords coords = new Coords(2, 3);
        for (int type : new int[] { Terrains.WOODS, Terrains.JUNGLE }) {
            Hex hex = new Hex(0);
            hex.addTerrain(new Terrain(Terrains.FOLIAGE_ELEV, 2));
            int previous = Integer.MAX_VALUE;
            for (int density = 3; density >= 0; density--) {
                if (density == 0) {
                    hex.removeTerrain(type);
                } else {
                    hex.addTerrain(new Terrain(type, density));
                }
                var features = BoardFeatures.capture(hex, coords, Map.of());
                int count = (int) features.stream().filter(feature -> feature.kind() == BoardScene.FeatureKind.TREE).count();
                assertTrue(count < previous, "Each cover reduction must visibly reduce the number of trees");
                assertEquals(density == 0, count == 0, "Trees disappear only when the hex becomes clear");
                assertEquals(features, BoardFeatures.capture(hex, coords, Map.of()), "Unchanged cover must keep its scenery");
                previous = count;
            }
        }
    }

    @Test
    void collectableLimbCountsControlStableGroundProps() {
        Hex hex = new Hex(0);
        Coords coords = new Coords(2, 3);
        hex.addTerrain(new Terrain(Terrains.ARMS, 2));
        hex.addTerrain(new Terrain(Terrains.LEGS, 1));
        var before = BoardFeatures.capture(hex, coords, Map.of()).stream()
              .filter(feature -> feature.kind() == BoardScene.FeatureKind.LIMB).toList();
        assertEquals(3, before.size());
        assertTrue(before.stream().allMatch(feature -> feature.kind() == BoardScene.FeatureKind.LIMB
              && feature.asset().equals("Limb Club")));
        assertEquals(3, before.stream().map(BoardScene.Feature::rotation).distinct().count());
        assertEquals(before, BoardFeatures.capture(hex, coords, Map.of()).stream()
              .filter(feature -> feature.kind() == BoardScene.FeatureKind.LIMB).toList());
        hex.addTerrain(new Terrain(Terrains.ARMS, 1));
        var after = BoardFeatures.capture(hex, coords, Map.of()).stream()
              .filter(feature -> feature.kind() == BoardScene.FeatureKind.LIMB).toList();
        assertEquals(2, after.size());
        assertTrue(before.containsAll(after), "Picking up one limb must not move the others");
        hex.removeTerrain(Terrains.ARMS);
        hex.removeTerrain(Terrains.LEGS);
        assertTrue(BoardFeatures.capture(hex, coords, Map.of()).stream()
              .noneMatch(feature -> feature.kind() == BoardScene.FeatureKind.LIMB));
    }

    @Test
    void snowTerrainAndThemeSelectSnowAssetsWhileJungleUsesPalms() {
        Hex hex = new Hex(0);
        hex.addTerrain(new Terrain(Terrains.JUNGLE, 2));
        hex.addTerrain(new Terrain(Terrains.FOLIAGE_ELEV, 2));
        Coords coords = new Coords(1, 1);
        assertTrue(BoardFeatures.capture(hex, coords, Map.of()).stream().allMatch(feature -> feature.asset().startsWith("palm")));
        hex.addTerrain(new Terrain(Terrains.SNOW, 1));
        assertTrue(BoardFeatures.capture(hex, coords, Map.of()).stream().allMatch(feature -> feature.asset().endsWith("-snow")));
        hex.removeTerrain(Terrains.SNOW);
        hex.setTheme("snow");
        assertTrue(BoardFeatures.capture(hex, coords, Map.of()).stream().allMatch(feature -> feature.asset().endsWith("-snow")));
        hex.setTheme("");
        assertFalse(BoardFeatures.capture(hex, coords, Map.of()).stream().anyMatch(feature -> feature.asset().endsWith("-snow")));
    }

    @Test
    void desertAndSandyWoodsUseOnlyPalmsAtEveryDensity() {
        Coords coords = new Coords(3, 2);
        for (int density = 1; density <= 3; density++) {
            Hex hex = new Hex(0);
            hex.addTerrain(new Terrain(Terrains.WOODS, density));
            hex.addTerrain(new Terrain(Terrains.FOLIAGE_ELEV, 2));
            hex.setTheme("Desert");
            var themed = BoardFeatures.capture(hex, coords, Map.of());
            assertFalse(themed.isEmpty());
            assertTrue(themed.stream().allMatch(feature -> feature.asset().equals("palm")
                  || feature.asset().equals("palm-bent")), "Desert woodland must retain palm silhouettes");
            hex.addTerrain(new Terrain(Terrains.PAVEMENT, 1));
            assertEquals(themed, BoardFeatures.capture(hex, coords, Map.of()), "Ground paving must not change the biome's trees");
            hex.removeTerrain(Terrains.PAVEMENT);
            hex.setTheme("");
            hex.addTerrain(new Terrain(Terrains.SAND, 1));
            assertEquals(themed, BoardFeatures.capture(hex, coords, Map.of()), "Sandy woods use the same palm selection");
            hex.addTerrain(new Terrain(Terrains.SNOW, 1));
            assertTrue(BoardFeatures.capture(hex, coords, Map.of()).stream().allMatch(feature -> feature.asset().endsWith("-snow")),
                  "Snow retains the existing winter variants");
        }
    }

    @Test
    void woodlandMixesSilhouettesWhileRubbleAndRoughAddOnlySmallScatter() {
        Hex hex = new Hex(0);
        Coords coords = new Coords(3, 2);
        hex.addTerrain(new Terrain(Terrains.WOODS, 2));
        hex.addTerrain(new Terrain(Terrains.FOLIAGE_ELEV, 2));
        assertTrue(BoardFeatures.capture(hex, coords, Map.of()).stream().map(BoardScene.Feature::asset).distinct().count() >= 5);
        for (String theme : new String[] { "", "snow", "desert" }) {
            for (int terrain : new int[] { Terrains.RUBBLE, Terrains.ROUGH }) {
                hex.removeAllTerrains();
                hex.setTheme(theme);
                hex.addTerrain(new Terrain(terrain, 4));
                assertTrue(BoardFeatures.capture(hex, coords, Map.of()).stream()
                      .allMatch(feature -> feature.kind() == BoardScene.FeatureKind.SCATTER));
                hex.addTerrain(new Terrain(Terrains.WOODS, 1));
                assertEquals(3, BoardFeatures.capture(hex, coords, Map.of()).size(),
                      "Cosmetic scatter must preserve coexisting woodland");
            }
        }
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
        String industrialRoof = "buildings/saxarba/misc/heavy_industrial_a";
        var models = Map.of(Terrains.BUILDING, selectedRoof, Terrains.INDUSTRIAL, industrialRoof);
        var features = BoardFeatures.capture(hex, new Coords(2, 2), models);
        assertTrue(features.stream().anyMatch(feature -> feature.asset().equals(selectedRoof) && feature.height() == 4));
        assertEquals(2, features.stream().filter(feature -> feature.asset().equals("bridge") && feature.elevation() == 2).count());
        assertTrue(features.stream().anyMatch(feature -> feature.asset().equals(industrialRoof) && feature.height() == 3));
        assertEquals(features, BoardFeatures.capture(hex, new Coords(2, 2), models), "Placement must remain stable across snapshots");
        assertFalse(BoardFeatures.capture(hex, new Coords(2, 2), Map.of()).stream()
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
        assertTrue(BoardFeatures.capture(hex, new Coords(0, 0), Map.of()).stream()
              .anyMatch(feature -> feature.asset().equals("field") && feature.height() == 1));
    }
}
