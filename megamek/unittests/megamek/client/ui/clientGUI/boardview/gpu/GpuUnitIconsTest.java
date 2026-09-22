/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.utils.GdxNativesLoader;
import org.junit.jupiter.api.Test;

class GpuUnitIconsTest {
    @Test
    void sharesMarkerTiltRuleAtEveryBearingAndRequiresDistantZoom() {
        GdxNativesLoader.load();
        BoardCamera camera = new BoardCamera();
        for (int bearing = 0; bearing < 360; bearing += 30) {
            for (float tilt : new float[] { 0, 29.9f, 30, 30.1f, 60 }) {
                camera.setIsometric(false);
                camera.orbit(bearing, tilt);
                boolean expected = GpuMarkers.flat(camera.camera);
                assertEquals(expected, GpuUnitIcons.useIcons(true, camera.camera, 40, 56, false));
                assertFalse(GpuUnitIcons.useIcons(false, camera.camera, 40, 56, true));
                assertFalse(GpuUnitIcons.useIcons(true, camera.camera, 80, 56, false));
            }
        }
    }

    @Test
    void zoomHysteresisPreventsChatterAndHonorsTheTunedThreshold() {
        GdxNativesLoader.load();
        BoardCamera camera = new BoardCamera();
        camera.setIsometric(false);
        assertTrue(GpuUnitIcons.useIcons(true, camera.camera, 56, 56, false));
        assertFalse(GpuUnitIcons.useIcons(true, camera.camera, 57, 56, false));
        assertTrue(GpuUnitIcons.useIcons(true, camera.camera, 60, 56, true));
        assertFalse(GpuUnitIcons.useIcons(true, camera.camera, 65, 56, true));
        assertTrue(GpuUnitIcons.useIcons(true, camera.camera, 80, 90, false));
        assertFalse(GpuUnitIcons.useIcons(true, camera.camera, 40, 24, true));
    }
}
