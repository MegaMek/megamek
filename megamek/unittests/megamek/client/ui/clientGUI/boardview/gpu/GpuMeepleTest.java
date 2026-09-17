/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;

import com.badlogic.gdx.graphics.Color;
import org.junit.jupiter.api.Test;

class GpuMeepleTest {
    @Test
    void sideColorIgnoresTransparentPixelsAndWeightsPartialAlpha() {
        BufferedImage image = new BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xffff0000);
        image.setRGB(1, 0, 0x800000ff);
        image.setRGB(2, 0, 0x0000ff00);
        Color color = GpuMeeple.averageColor(new BoardScene.Pixels(image));
        assertEquals(255f / 383, color.r, 0.0001f);
        assertEquals(0, color.g);
        assertEquals(128f / 383, color.b, 0.0001f);
        assertEquals(1, color.a);
    }

    @Test
    void emptySpriteHasFiniteOpaqueSideColor() {
        assertEquals(Color.BLACK, GpuMeeple.averageColor(new BoardScene.Pixels(
              new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB))));
    }
}
