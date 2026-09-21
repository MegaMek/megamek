/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.math.Vector2;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Test;

class BoardFlowTest {
    @Test
    void flatLakesAndAmbiguousFlatRiversHaveNoInventedCurrent() {
        Map<Coords, Integer> water = new HashMap<>();
        for (int y = 0; y < 12; y++) { water.put(new Coords(2, y), 0); }
        Coords lake = new Coords(8, 6);
        for (int x = 6; x <= 10; x++) {
            for (int y = 4; y <= 8; y++) {
                if (lake.distance(new Coords(x, y)) <= 2) { water.put(new Coords(x, y), 0); }
            }
        }
        assertTrue(BoardFlow.calculate(scene(12, 12, water, Map.of(), Set.of())).isEmpty());
    }

    @Test
    void currentsFollowAFlatBendTowardItsLowerOutletAndReverseWhenTheOutletChanges() {
        List<Coords> river = new ArrayList<>(List.of(new Coords(3, 1)));
        for (int direction : new int[] { 3, 3, 2, 2, 3 }) { river.add(river.getLast().translated(direction)); }
        Map<Coords, Integer> water = new HashMap<>();
        river.forEach(coords -> water.put(coords, 2));
        water.put(river.getLast(), 0);
        Map<Coords, BoardFlow.Current> forward = BoardFlow.calculate(scene(12, 12, water, Map.of(), Set.of()));
        for (int index = 0; index < river.size() - 1; index++) {
            assertDownstream(forward.get(river.get(index)), river.get(index), river.get(index + 1));
        }
        assertFalse(forward.containsKey(river.getLast()), "The closed lower pool has no assumed outlet");
        water.put(river.getLast(), 2);
        water.put(river.getFirst(), 0);
        Map<Coords, BoardFlow.Current> reverse = BoardFlow.calculate(scene(12, 12, water, Map.of(), Set.of()));
        for (int index = 1; index < river.size(); index++) {
            assertDownstream(reverse.get(river.get(index)), river.get(index), river.get(index - 1));
        }
    }

    @Test
    void aHigherInletDoesNotMakeTheLakeBodyDrift() {
        Map<Coords, Integer> water = new HashMap<>();
        Coords center = new Coords(6, 6), inlet = new Coords(6, 3);
        for (int x = 3; x <= 9; x++) {
            for (int y = 3; y <= 9; y++) {
                Coords coords = new Coords(x, y);
                if (center.distance(coords) <= 2) { water.put(coords, 0); }
            }
        }
        water.put(inlet, 2);
        Map<Coords, BoardFlow.Current> currents = BoardFlow.calculate(scene(12, 12, water, Map.of(), Set.of()));
        assertEquals(Set.of(inlet), currents.keySet());
    }

    @Test
    void iceAndIncompatibleLiquidsStopTheCurrentButToxicWaterSharesIt() {
        Map<Coords, Integer> water = Map.of(new Coords(3, 2), 2, new Coords(3, 3), 2, new Coords(3, 4), 0);
        assertTrue(BoardFlow.calculate(scene(8, 8, water, Map.of(), Set.of(new Coords(3, 3)))).isEmpty());
        var lava = new BoardLiquid(BoardLiquid.Kind.MAGMA, "", 0);
        assertTrue(BoardFlow.calculate(scene(8, 8, water, Map.of(new Coords(3, 4), lava), Set.of())).isEmpty());
        var toxic = new BoardLiquid(BoardLiquid.Kind.HAZARDOUS, "", 0);
        assertDownstream(BoardFlow.calculate(scene(8, 8, water, Map.of(new Coords(3, 4), toxic), Set.of()))
              .get(new Coords(3, 2)), new Coords(3, 2), new Coords(3, 3));
    }

    @Test
    void flowAcceleratesNearTheConnectedWaterfallAndRespondsToItsDropHeight() {
        Map<Coords, Integer> water = new HashMap<>();
        for (int y = 1; y <= 8; y++) { water.put(new Coords(3, y), 2); }
        Coords outlet = new Coords(3, 8), lip = new Coords(3, 7);
        water.put(outlet, 1);
        var currents = BoardFlow.calculate(scene(8, 10, water, Map.of(), Set.of()));
        assertEquals(speed(currents.get(new Coords(3, 1))), speed(currents.get(new Coords(3, 4))), 0.0001f,
              "A waterfall must not accelerate the whole river");
        for (int y = 4; y < 7; y++) {
            assertTrue(speed(currents.get(new Coords(3, y + 1))) > speed(currents.get(new Coords(3, y))),
                  "Each approach hex must flow faster toward the lip");
        }
        water.put(outlet, -2);
        var largerDrop = BoardFlow.calculate(scene(8, 10, water, Map.of(), Set.of()));
        assertTrue(speed(largerDrop.get(lip)) > speed(currents.get(lip)), "Larger drops pull a faster current");
        water.put(outlet, -100);
        var extremeDrop = BoardFlow.calculate(scene(8, 10, water, Map.of(), Set.of()));
        assertEquals(speed(largerDrop.get(lip)), speed(extremeDrop.get(lip)), 0.0001f,
              "Extreme map elevations must not create unbounded animation speeds");
        assertFalse(extremeDrop.containsKey(outlet), "The closed receiving pool remains in place");
    }

    private static float speed(BoardFlow.Current current) {
        return new Vector2(current.u(), current.v() * BoardGeometry.HEIGHT / BoardGeometry.WIDTH).len();
    }

    @Test
    void distantOutletEditsReverseAnUnchangedReachAcrossChunkBoundaries() {
        Map<Coords, Integer> water = new HashMap<>();
        for (int y = 1; y <= 63; y++) { water.put(new Coords(3, y), 1); }
        water.put(new Coords(3, 63), 0);
        var south = BoardFlow.calculate(scene(8, 65, water, Map.of(), Set.of()));
        water.put(new Coords(3, 63), 1);
        water.put(new Coords(3, 1), 0);
        var north = BoardFlow.calculate(scene(8, 65, water, Map.of(), Set.of()));
        assertDownstream(south.get(new Coords(3, 32)), new Coords(3, 32), new Coords(3, 33));
        assertDownstream(north.get(new Coords(3, 32)), new Coords(3, 32), new Coords(3, 31));
    }

    private static void assertDownstream(BoardFlow.Current current, Coords from, Coords to) {
        assertTrue(current != null, "Missing current at " + from);
        Vector2 velocity = new Vector2(-current.u() * BoardGeometry.WIDTH, current.v() * BoardGeometry.HEIGHT).nor();
        Vector2 direction = new Vector2(BoardGeometry.centerX(to) - BoardGeometry.centerX(from),
              BoardGeometry.centerY(to) - BoardGeometry.centerY(from)).nor();
        assertTrue(velocity.dot(direction) > 0.7f, "Stream must head toward its outlet at " + from);
    }

    private static BoardScene scene(int width, int height, Map<Coords, Integer> water, Map<Coords, BoardLiquid> types,
          Set<Coords> frozen) {
        BoardScene.Pixels pixels = new BoardScene.Pixels(new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB));
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Coords coords = new Coords(x, y);
                boolean wet = water.containsKey(coords);
                tiles.add(new BoardScene.Tile(coords, water.getOrDefault(coords, 0), wet ? 1 : -1, frozen.contains(coords),
                      0, BoardScene.Surface.ROCK, pixels, null, null, null, null, List.of(), List.of(),
                      wet ? types.getOrDefault(coords, BoardLiquid.WATER) : BoardLiquid.NONE));
            }
        }
        return new BoardScene(0, width, height, tiles, List.of(), List.of(), -1, "", List.of());
    }
}
