/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.math.Vector2;
import megamek.common.board.Coords;

/** Visual currents inferred from connected surface elevations. Never supplies game movement or terrain rules. */
final class BoardFlow {
    private static final int WATERFALL_APPROACH_HEXES = 3;

    /** Texture displacement per second; zero leaves the authored surface animation in place. */
    record Current(float u, float v) {
        static final Current STILL = new Current(0, 0);
    }

    private BoardFlow() { }

    static Map<Coords, Current> calculate(BoardScene scene) {
        Map<Coords, List<BoardScene.Tile>> neighbors = new HashMap<>();
        Set<Coords> lakes = new HashSet<>();
        for (BoardScene.Tile tile : scene.tiles()) {
            if (!tile.liquid().present() || tile.frozen()) { continue; }
            List<BoardScene.Tile> adjacent = new ArrayList<>();
            int levelMouths = 0;
            for (int direction = 0; direction < 6; direction++) {
                BoardScene.Tile neighbor = scene.tile(tile.coords().translated(direction));
                if (neighbor != null && !neighbor.frozen() && tile.liquid().connects(neighbor.liquid())) {
                    adjacent.add(neighbor);
                    if (neighbor.elevation() == tile.elevation()) { levelMouths |= 1 << direction; }
                }
            }
            neighbors.put(tile.coords(), adjacent);
            // Adjacent open mouths form broad bays; separated mouths and narrow junctions carry a stream.
            if ((levelMouths & ((levelMouths << 1) | (levelMouths >> 5))) != 0) { lakes.add(tile.coords()); }
        }
        Map<Coords, Coords> downstream = new HashMap<>();
        Set<Coords> visited = new HashSet<>();
        for (BoardScene.Tile start : scene.tiles()) {
            if (!neighbors.containsKey(start.coords()) || !visited.add(start.coords())) { continue; }
            List<BoardScene.Tile> plateau = new ArrayList<>();
            ArrayDeque<BoardScene.Tile> pending = new ArrayDeque<>();
            pending.add(start);
            while (!pending.isEmpty()) {
                BoardScene.Tile tile = pending.removeFirst();
                plateau.add(tile);
                for (BoardScene.Tile neighbor : neighbors.get(tile.coords())) {
                    if (neighbor.elevation() == start.elevation() && visited.add(neighbor.coords())) { pending.add(neighbor); }
                }
            }
            Map<Coords, Integer> distance = new HashMap<>();
            boolean hasInlet = false;
            List<BoardScene.Tile> boundary = new ArrayList<>();
            for (BoardScene.Tile tile : plateau) {
                BoardScene.Tile lowest = null;
                for (BoardScene.Tile neighbor : neighbors.get(tile.coords())) {
                    hasInlet |= neighbor.elevation() > tile.elevation();
                    if (neighbor.elevation() < tile.elevation() && (lowest == null || neighbor.elevation() < lowest.elevation())) {
                        lowest = neighbor;
                    }
                }
                if (lowest != null) {
                    downstream.put(tile.coords(), lowest.coords());
                    distance.put(tile.coords(), 0);
                    pending.add(tile);
                }
                if (tile.coords().getX() == 0 || tile.coords().getX() == scene.width() - 1
                      || tile.coords().getY() == 0 || tile.coords().getY() == scene.height() - 1) { boundary.add(tile); }
            }
            // A higher inlet can identify a single boundary outlet or the lake into which a flat reach empties.
            // With no elevation evidence, even a narrow edge-to-edge river has no assumed direction.
            if (pending.isEmpty() && hasInlet) {
                if (boundary.size() == 1 && !lakes.contains(boundary.getFirst().coords())) {
                    BoardScene.Tile outlet = boundary.getFirst();
                    for (int direction = 0; direction < 6; direction++) {
                        Coords outside = outlet.coords().translated(direction);
                        if (scene.tile(outside) == null) { downstream.put(outlet.coords(), outside); break; }
                    }
                    distance.put(outlet.coords(), 0);
                    pending.add(outlet);
                } else {
                    for (BoardScene.Tile tile : plateau) {
                        if (lakes.contains(tile.coords())) { distance.put(tile.coords(), 0); pending.add(tile); }
                    }
                }
            }
            while (!pending.isEmpty()) {
                BoardScene.Tile tile = pending.removeFirst();
                for (BoardScene.Tile neighbor : neighbors.get(tile.coords())) {
                    if (neighbor.elevation() == tile.elevation() && !distance.containsKey(neighbor.coords())) {
                        distance.put(neighbor.coords(), distance.get(tile.coords()) + 1);
                        downstream.put(neighbor.coords(), tile.coords());
                        pending.add(neighbor);
                    }
                }
            }
        }
        Map<Coords, Current> result = new HashMap<>();
        for (var entry : downstream.entrySet()) {
            BoardScene.Tile tile = scene.tile(entry.getKey()), target = scene.tile(entry.getValue());
            if (lakes.contains(tile.coords()) && (target == null || target.elevation() == tile.elevation())) { continue; }
            Vector2 direction = direction(tile.coords(), entry.getValue());
            Vector2 incoming = new Vector2();
            for (BoardScene.Tile neighbor : neighbors.get(tile.coords())) {
                if (tile.coords().equals(downstream.get(neighbor.coords()))) { incoming.add(direction(neighbor.coords(), tile.coords())); }
            }
            if (!incoming.isZero()) { direction.add(incoming.nor()).nor(); }
            float speed = tile.liquid().molten() ? 0.025f : 0.10f + 0.04f * tile.liquid().rapids();
            speed *= waterfallSpeed(scene, tile, downstream, lakes);
            // UV V points toward world -Y. Offsetting against the velocity moves the painted features downstream.
            result.put(tile.coords(), new Current(-direction.x * speed, direction.y * speed * BoardGeometry.WIDTH / BoardGeometry.HEIGHT));
        }
        return Map.copyOf(result);
    }

    /** Follow the connected stream, not straight-line proximity to an unrelated waterfall. */
    private static float waterfallSpeed(BoardScene scene, BoardScene.Tile tile, Map<Coords, Coords> downstream, Set<Coords> lakes) {
        for (int distance = 0; distance < WATERFALL_APPROACH_HEXES; distance++) {
            Coords next = downstream.get(tile.coords());
            BoardScene.Tile target = next == null ? null : scene.tile(next);
            if (target == null) { break; }
            int drop = tile.elevation() - target.elevation();
            if (drop > 0) {
                // Bounded artistic acceleration, fading upstream over three hexes; no change to GIF timing.
                float proximity = 1 - distance / (float) WATERFALL_APPROACH_HEXES;
                return 1 + 1.25f * (float) Math.sqrt(Math.min(drop, 4)) * proximity;
            }
            if (lakes.contains(tile.coords())) { break; }
            tile = target;
        }
        return 1;
    }

    private static Vector2 direction(Coords from, Coords to) {
        return new Vector2(BoardGeometry.centerX(to) - BoardGeometry.centerX(from),
              BoardGeometry.centerY(to) - BoardGeometry.centerY(from)).nor();
    }
}
