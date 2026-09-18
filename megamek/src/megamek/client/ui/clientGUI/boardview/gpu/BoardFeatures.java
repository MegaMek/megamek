/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import megamek.common.Hex;
import megamek.common.board.Coords;
import megamek.common.units.Terrains;

/** Copies terrain appearance on the Swing thread; no game objects cross into the renderer. */
final class BoardFeatures {
    private static final List<String> TREES = List.of("tree", "pine", "tree-broad", "birch", "tree-slender", "pine-tall", "willow");
    private static final List<String> ROCKS = List.of("rock-1", "rock-3", "rock-6");
    private BoardFeatures() { }

    static BoardScene.Surface surface(Hex hex) {
        String theme = hex.getTheme() == null ? "" : hex.getTheme().toLowerCase(Locale.ROOT);
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

    static List<BoardScene.Feature> capture(Hex hex, Coords coords, String buildingModel) {
        List<BoardScene.Feature> result = new ArrayList<>();
        int variant = Math.floorMod(coords.getX() * 31 + coords.getY() * 17, 4);
        if (hex.containsTerrain(Terrains.BUILDING) && !buildingModel.isEmpty()) {
            add(result, buildingModel, Math.max(1, hex.terrainLevel(Terrains.BLDG_ELEV)), 0, 0);
        }
        if (hex.containsTerrain(Terrains.FUEL_TANK)) {
            add(result, "tank", Math.max(1, hex.terrainLevel(Terrains.FUEL_TANK_ELEV)), 0, 0);
        }
        if (hex.containsTerrain(Terrains.INDUSTRIAL)) {
            add(result, "industrial", Math.max(1, hex.terrainLevel(Terrains.INDUSTRIAL)), 0, 0);
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
                      height * (0.8f + (index % 3) * 0.1f), 0));
            }
        }
        if (hex.containsTerrain(Terrains.RUBBLE)) {
            boolean snow = surface(hex) == BoardScene.Surface.SNOW;
            int count = hex.terrainLevel(Terrains.RUBBLE) >= 3 ? 8 : 5;
            for (int index = 0; index < count; index++) {
                double angle = index * 2.399963 + variant;
                float radius = index == 0 ? 0 : 7 + index * 2.4f;
                String rock = ROCKS.get((index + variant) % ROCKS.size()) + (snow ? "-snow" : "");
                result.add(new BoardScene.Feature(rock, (float) Math.cos(angle) * radius,
                      (float) Math.sin(angle) * radius, index * 137.5f, 0.8f + (index % 3) * 0.2f,
                      0.16f + (index % 4) * 0.035f, 0));
            }
        }
        return List.copyOf(result);
    }

    private static boolean desert(Hex hex) {
        String theme = hex.getTheme() == null ? "" : hex.getTheme().toLowerCase(Locale.ROOT);
        return hex.containsTerrain(Terrains.SAND) || theme.contains("desert") || theme.contains("sand");
    }

    private static void add(List<BoardScene.Feature> result, String asset, float height, float elevation, float rotation) {
        result.add(new BoardScene.Feature(asset, 0, 0, rotation, 1, height, elevation));
    }
}
