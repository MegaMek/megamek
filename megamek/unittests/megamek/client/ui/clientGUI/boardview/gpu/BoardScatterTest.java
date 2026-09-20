/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.Hex;
import megamek.common.board.Coords;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Test;

class BoardScatterTest {
    @Test
    void naturalSurfacesStaySparseVariedAndStableAcrossSnapshots() {
        for (String theme : List.of("grass", "lunar", "desert", "dirt", "snow", "mars", "volcanic")) {
            Hex hex = new Hex(0);
            hex.setTheme(theme);
            int occupied = 0;
            int pairs = 0;
            Set<String> shapes = new HashSet<>();
            Set<Float> rotations = new HashSet<>();
            for (int x = 0; x < 64; x++) {
                for (int y = 0; y < 64; y++) {
                    Coords coords = new Coords(x, y);
                    var features = scatter(hex, coords);
                    assertTrue(features.size() <= 2);
                    assertEquals(features, scatter(hex, coords));
                    // Collectable limbs must not change the cosmetic ground layout.
                    hex.addTerrain(new Terrain(Terrains.ARMS, 1));
                    assertEquals(features, scatter(hex, coords));
                    hex.removeTerrain(Terrains.ARMS);
                    if (!features.isEmpty()) {
                        occupied++;
                    }
                    if (features.size() == 2) {
                        pairs++;
                    }
                    for (var feature : features) {
                        shapes.add(feature.asset());
                        rotations.add(feature.rotation());
                        assertTrue(Math.hypot(feature.x(), feature.y()) + 4 * feature.scale() < 32,
                              "The entire detail stays clear of hex edges and cliffs");
                        assertTrue(feature.height() < .2f, "Scatter cannot resemble gameplay-height obstacles");
                    }
                }
            }
            double baseline = switch (theme) {
                case "grass" -> .16;
                case "lunar", "volcanic" -> .18;
                case "dirt", "mars" -> .12;
                case "desert" -> .10;
                default -> .06;
            };
            double expected = Math.clamp(baseline * BoardFeatures.SCATTER_DENSITY_MULTIPLIER, 0, 1);
            assertEquals(expected, occupied / 4096.0, .03, theme + ": " + occupied + " occupied of 4096 hexes");
            if (occupied == 0) {
                continue;
            }
            assertTrue(pairs > 0 && pairs < occupied / 3, "Pairs remain occasional");
            assertTrue(rotations.size() > occupied, "No small repeating rotation set");
            assertTrue(shapes.containsAll(List.of("scatter-rock", "scatter-slab")), theme);
            if (theme.equals("grass")) {
                assertTrue(shapes.containsAll(List.of("scatter-grass", "scatter-plant")));
            } else if (theme.equals("desert")) {
                assertTrue(shapes.contains("scatter-plant"));
                assertFalse(shapes.contains("scatter-grass"));
            } else if (theme.equals("dirt")) {
                assertTrue(shapes.contains("scatter-dry-grass"));
            } else {
                assertEquals(Set.of("scatter-rock", "scatter-slab"), shapes);
            }
        }
    }

    @Test
    void tundraUsesDryTuftsInsteadOfLushPlants() {
        Hex hex = new Hex(0);
        hex.addTerrain(new Terrain(Terrains.TUNDRA, 1));
        Set<String> shapes = new HashSet<>();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                scatter(hex, new Coords(x, y)).forEach(feature -> shapes.add(feature.asset()));
            }
        }
        assertEquals(BoardFeatures.SCATTER_DENSITY_MULTIPLIER <= 0 ? Set.of()
              : Set.of("scatter-dry-grass", "scatter-rock", "scatter-slab"), shapes);
    }

    @Test
    void structuresRoadsWaterAndExistingVegetationStayClear() {
        // Check every protected type over many coordinates, including hexes that would otherwise get scatter.
        for (int type : new int[] { Terrains.WATER, Terrains.ICE, Terrains.ROAD, Terrains.PAVEMENT, Terrains.BRIDGE,
              Terrains.BUILDING, Terrains.FUEL_TANK, Terrains.INDUSTRIAL, Terrains.FIELDS, Terrains.WOODS, Terrains.JUNGLE,
              Terrains.SPACE, Terrains.SKY, Terrains.MAGMA, Terrains.FIRE, Terrains.GEYSER, Terrains.SWAMP, Terrains.MUD,
              Terrains.HAZARDOUS_LIQUID, Terrains.FORTIFIED }) {
            Hex hex = new Hex(0);
            hex.addTerrain(new Terrain(type, type == Terrains.WATER ? 0 : 1));
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    assertTrue(scatter(hex, new Coords(x, y)).isEmpty(), "Protected terrain " + type);
                }
            }
        }
    }

    private static List<BoardScene.Feature> scatter(Hex hex, Coords coords) {
        return BoardFeatures.capture(hex, coords, Map.of()).stream()
              .filter(feature -> feature.kind() == BoardScene.FeatureKind.SCATTER).toList();
    }
}
