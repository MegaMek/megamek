/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import megamek.common.Hex;
import megamek.common.board.Coords;
import megamek.common.units.Terrains;

/** Copies terrain appearance on the Swing thread; no game objects cross into the renderer. */
final class BoardFeatures {
    /** Global scatter density: 0 disables it, 1 is the baseline, 3 triples each biome's placement chance. */
    static final float SCATTER_DENSITY_MULTIPLIER = 3.0f;
    private static final List<String> TREES = List.of("tree", "pine", "tree-broad", "birch", "tree-slender", "pine-tall", "willow");
    private BoardFeatures() { }

    static BoardScene.Surface surface(Hex hex) {
        String theme = hex.getTheme() == null ? "" : hex.getTheme().toLowerCase(Locale.ROOT);
        if (hex.containsTerrain(Terrains.MAGMA)) { return BoardScene.Surface.ROCK; }
        if (hex.containsTerrain(Terrains.SNOW) || theme.contains("snow")) {
            return BoardScene.Surface.SNOW;
        }
        if (hex.containsTerrain(Terrains.PAVEMENT)) {
            return BoardScene.Surface.CONCRETE;
        }
        if (desert(hex)) {
            return BoardScene.Surface.SAND;
        }
        if (hex.containsAnyTerrainOf(Terrains.ROUGH, Terrains.RUBBLE)
              || theme.contains("lunar") || theme.contains("rock") || theme.contains("volcan")) {
            return BoardScene.Surface.ROCK;
        }
        if (hex.containsAnyTerrainOf(Terrains.MUD, Terrains.SWAMP) || theme.contains("dirt") || theme.contains("mars")) {
            return BoardScene.Surface.DIRT;
        }
        return BoardScene.Surface.GRASS;
    }

    static List<BoardScene.Feature> capture(Hex hex, Coords coords, Map<Integer, String> structureModels) {
        List<BoardScene.Feature> result = new ArrayList<>();
        int variant = Math.floorMod(coords.getX() * 31 + coords.getY() * 17, 4);
        // Terrain levels are the game's collectable limb counts, not damage inferred from nearby units.
        for (int type = 0; type < 2; type++) {
            int count = Math.max(0, hex.terrainLevel(type == 0 ? Terrains.ARMS : Terrains.LEGS));
            for (int index = 0; index < count; index++) {
                int slot = index * 2 + type;
                double angle = slot * 2.399963 + variant;
                float radius = 10 + slot % 4 * 4;
                result.add(new BoardScene.Feature("Limb Club", (float) Math.cos(angle) * radius,
                      (float) Math.sin(angle) * radius, (float) Math.toDegrees(angle), 1, 1, 0,
                      BoardScene.FeatureKind.LIMB));
            }
        }
        for (var structure : structureModels.entrySet()) {
            int heightTerrain = switch (structure.getKey()) {
                case Terrains.BUILDING -> Terrains.BLDG_ELEV;
                case Terrains.FUEL_TANK -> Terrains.FUEL_TANK_ELEV;
                default -> Terrains.INDUSTRIAL;
            };
            result.add(new BoardScene.Feature(structure.getValue(), 0, 0, 0, 1,
                  Math.max(1, hex.terrainLevel(heightTerrain)), 0, structure.getKey() == Terrains.BUILDING
                        ? BoardScene.FeatureKind.BUILDING : BoardScene.FeatureKind.PROP));
        }
        if (hex.containsTerrain(Terrains.FIELDS)) {
            add(result, "field", 1, 0, 0);
        }
        if (hex.containsTerrain(Terrains.BRIDGE)) {
            int exits = hex.getTerrain(Terrains.BRIDGE).getExits() & 63;
            for (int direction = 0; direction < 6; direction++) {
                if ((exits & (1 << direction)) != 0) {
                    Coords neighbor = coords.translated(direction);
                    float dx = (neighbor.getX() - coords.getX()) * BoardGeometry.TILE_WIDTH * 0.75f;
                    float dy = -((neighbor.getY() - coords.getY()) * BoardGeometry.TILE_HEIGHT
                          + ((neighbor.getX() & 1) - (coords.getX() & 1)) * BoardGeometry.TILE_HEIGHT / 2);
                    result.add(new BoardScene.Feature("bridge", 0, 0, (float) Math.toDegrees(Math.atan2(-dx, dy)),
                          (float) Math.hypot(dx, dy) / BoardGeometry.TILE_HEIGHT, 1,
                          hex.terrainLevel(Terrains.BRIDGE_ELEV)));
                }
            }
        }
        boolean jungle = hex.containsTerrain(Terrains.JUNGLE);
        if (jungle || hex.containsTerrain(Terrains.WOODS)) {
            boolean snow = surface(hex) == BoardScene.Surface.SNOW;
            boolean palms = !snow && (jungle || desert(hex));
            int density = hex.terrainLevel(jungle ? Terrains.JUNGLE : Terrains.WOODS);
            int count = density >= 3 ? 16 : density == 2 ? 9 : 3;
            float height = Math.max(1, hex.terrainLevel(Terrains.FOLIAGE_ELEV));
            for (int index = 0; index < count; index++) {
                double angle = index * (density >= 2 ? 2.399963 : 2 * Math.PI / count) + variant;
                // Space light foliage around the centre; dense foliage fills an equal-area spiral.
                float radius = density >= 2 ? 28 * (float) Math.sqrt(index / (count - 1f))
                      : 20 + index * 2;
                String tree = palms ? (index % 2 == 0 ? "palm" : "palm-bent")
                      : TREES.get(Math.floorMod(coords.getX() * 31 + coords.getY() * 17 + index, TREES.size()));
                if (snow) {
                    tree += "-snow";
                }
                result.add(new BoardScene.Feature(tree, (float) Math.cos(angle) * radius,
                      (float) Math.sin(angle) * radius, index * 137.5f, 0.8f + (index % 3) * 0.1f,
                      height * (0.8f + (index % 3) * 0.1f), 0, BoardScene.FeatureKind.TREE));
            }
        }
        scatter(hex, coords, result);
        return List.copyOf(result);
    }

    /** Cosmetic only: most hexes stay empty, and terrain updates never reshuffle neighboring details. */
    private static void scatter(Hex hex, Coords coords, List<BoardScene.Feature> result) {
        if (hex.containsAnyTerrainOf(Terrains.WATER, Terrains.ICE, Terrains.ROAD, Terrains.PAVEMENT,
              Terrains.BRIDGE, Terrains.BUILDING, Terrains.FUEL_TANK, Terrains.INDUSTRIAL, Terrains.FIELDS,
              Terrains.WOODS, Terrains.JUNGLE, Terrains.SPACE, Terrains.SKY, Terrains.MAGMA, Terrains.FIRE,
              Terrains.GEYSER, Terrains.SWAMP, Terrains.MUD, Terrains.HAZARDOUS_LIQUID, Terrains.FORTIFIED)) {
            return;
        }
        BoardScene.Surface surface = surface(hex);
        float density = switch (surface) {
            case GRASS -> .16f;
            case ROCK -> .18f;
            case DIRT -> .12f;
            case SAND -> .10f;
            case SNOW -> .06f;
            case CONCRETE -> 0;
        };
        Random random = new Random(coords.getX() * 0x9E3779B97F4A7C15L
              ^ coords.getY() * 0xC2B2AE3D27D4EB4FL ^ 0x165667B19E3779F9L);
        if (random.nextFloat() >= density * SCATTER_DENSITY_MULTIPLIER) {
            return;
        }
        int count = random.nextFloat() < .2f ? 2 : 1;
        String theme = hex.getTheme() == null ? "" : hex.getTheme().toLowerCase(Locale.ROOT);
        boolean plants = !theme.contains("lunar") && !theme.contains("mars") && !theme.contains("volcan");
        for (int index = 0; index < count; index++) {
            int choice = random.nextInt(10);
            String asset = choice % 2 == 0 ? "scatter-rock" : "scatter-slab";
            if (plants && surface == BoardScene.Surface.GRASS) {
                if (choice < 5 || hex.containsTerrain(Terrains.TUNDRA) && choice < 8) {
                    asset = hex.containsTerrain(Terrains.TUNDRA) ? "scatter-dry-grass" : "scatter-grass";
                } else if (choice < 8) {
                    asset = "scatter-plant";
                }
            } else if (plants && surface == BoardScene.Surface.DIRT && choice < 4) {
                asset = "scatter-dry-grass";
            } else if (plants && surface == BoardScene.Surface.SAND && choice == 0) {
                asset = "scatter-plant";
            }
            double angle = random.nextDouble() * Math.PI * 2;
            float radius = 24 * (float) Math.sqrt(random.nextFloat());
            result.add(new BoardScene.Feature(asset, (float) Math.cos(angle) * radius,
                  (float) Math.sin(angle) * radius, random.nextFloat() * 360, .7f + random.nextFloat() * .5f,
                  .09f + random.nextFloat() * .09f, 0, BoardScene.FeatureKind.SCATTER));
        }
    }

    private static boolean desert(Hex hex) {
        String theme = hex.getTheme() == null ? "" : hex.getTheme().toLowerCase(Locale.ROOT);
        return hex.containsTerrain(Terrains.SAND) || theme.contains("desert") || theme.contains("sand");
    }

    private static void add(List<BoardScene.Feature> result, String asset, float height, float elevation, float rotation) {
        result.add(new BoardScene.Feature(asset, 0, 0, rotation, 1, height, elevation));
    }
}
