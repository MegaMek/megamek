/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

class GpuGroundWeatherTest {
    @Test
    void fogAndSandKeepBaseFlowWhenCalmAndFollowStrongerWind() {
        var calm = new GpuAtmosphere.GroundMotion();
        calm.advance(BoardAtmosphere.Effects.NONE, 0.1f);
        assertTrue(calm.fog.len() > 0);
        var still = new GpuAtmosphere.GroundMotion();
        still.advance(BoardAtmosphere.Effects.NONE, 0.1f, 0);
        assertEquals(Vector2.Zero, still.fog, "Calm drift remains optional for visual comparisons");
        assertTrue(calm.sand.y > 0 && calm.grains > 0);
        for (int direction : new int[] { 0, 90, 180, 270 }) {
            float previous = 0;
            for (float wind : new float[] { 0.2f, 1 }) {
                var motion = new GpuAtmosphere.GroundMotion();
                motion.advance(wind(wind, direction), 0.1f);
                Vector2 travel = signed(motion.fog);
                float along = switch (direction) {
                    case 0 -> travel.y;
                    case 90 -> travel.x;
                    case 180 -> -travel.y;
                    default -> -travel.x;
                };
                assertTrue(along > previous, "Stronger wind must accelerate banks in the selected direction");
                assertEquals(along, travel.len(), 0.0001f, "Fog cannot drift across the selected wind");
                assertTrue(signed(motion.sand).dot(travel) > 0, "Sand must follow the same wind direction");
                previous = along;
            }
        }
    }

    @Test
    void travelIsContinuousAcrossFramesWindChangesAndPeriodicWraps() {
        var single = new GpuAtmosphere.GroundMotion();
        var split = new GpuAtmosphere.GroundMotion();
        single.advance(wind(0.6f, 60), 0.1f);
        for (int i = 0; i < 10; i++) { split.advance(wind(0.6f, 60), 0.01f); }
        assertTrue(single.fog.epsilonEquals(split.fog, 0.0001f));
        assertTrue(single.sand.epsilonEquals(split.sand, 0.0001f));
        assertEquals(single.grains, split.grains, 0.0001f);
        split.advance(wind(1, 240), 0);
        assertTrue(single.fog.epsilonEquals(split.fog, 0.0001f), "Changing wind cannot teleport existing banks");
        split.advance(wind(1, 240), Float.NaN);
        assertTrue(single.fog.epsilonEquals(split.fog, 0.0001f));
        split.fog.set(0, 255.999f);
        split.advance(wind(1, 0), 0.1f);
        assertTrue(split.fog.y > 0 && split.fog.y < 0.02f, "Wrap at the exact repeating texture period");
    }

    @Test
    void calmFogDirectionVariesSmoothlyAndIsIndependentOfFrameRate() {
        var regular = new GpuAtmosphere.GroundMotion();
        var fast = new GpuAtmosphere.GroundMotion();
        Vector2 first = null;
        Vector2 last = null;
        for (int i = 0; i < 120; i++) {
            Vector2 before = regular.fog.cpy();
            regular.advance(BoardAtmosphere.Effects.NONE, 0.1f);
            for (int j = 0; j < 10; j++) { fast.advance(BoardAtmosphere.Effects.NONE, 0.01f); }
            last = regular.fog.cpy().sub(before);
            if (first == null) { first = last.cpy(); }
            assertTrue(last.len() < 0.004f, "Calm advection stays gentle without random frame jitter");
        }
        assertTrue(regular.fog.epsilonEquals(fast.fog, 0.0001f));
        assertTrue(Math.abs(first.nor().crs(last.nor())) > 0.3f, "Calm banks follow a changing flow vector");
    }

    @Test
    void variationsCanBeDisabledAndRemainBounded() {
        var uniform = new GpuAtmosphere.Options(0, false, 0, 1, 0, -1, -1);
        assertEquals(0, uniform.fogHeightVariation());
        assertEquals(0, uniform.fogDensityVariation());
        var maximum = new GpuAtmosphere.Options(0, false, 0, 1, 0, 100, 100);
        assertEquals(4, maximum.fogHeightVariation());
        assertEquals(1, maximum.fogDensityVariation());
        assertThrows(IllegalArgumentException.class, () -> new GpuAtmosphere.Options(0, false, 0, 1, 0, Float.NaN, 1));
        assertThrows(IllegalArgumentException.class,
              () -> new GpuAtmosphere.Options(0, false, 0, 1, 0, 1, Float.POSITIVE_INFINITY));
        assertFalse(new BoardAtmosphere.Effects(0, 0, 0, 1, 0, 0, 0).hasParticles(),
              "Sand cannot allocate or draw the precipitation pool");
    }

    private static BoardAtmosphere.Effects wind(float strength, float direction) {
        return new BoardAtmosphere.Effects(0, 0, 0, 0, 0, strength, direction);
    }

    private static Vector2 signed(Vector2 wrapped) {
        return new Vector2(wrapped.x > 128 ? wrapped.x - 256 : wrapped.x, wrapped.y > 128 ? wrapped.y - 256 : wrapped.y);
    }
}
