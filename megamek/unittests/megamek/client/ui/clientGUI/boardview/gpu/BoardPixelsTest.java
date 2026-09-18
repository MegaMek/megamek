/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;

class BoardPixelsTest {
    @Test
    void copiesSubimageStridesAndConvertsOtherImageFormatsExactly() {
        for (int type : new int[] { BufferedImage.TYPE_INT_ARGB, BufferedImage.TYPE_INT_ARGB_PRE,
              BufferedImage.TYPE_4BYTE_ABGR }) {
            BufferedImage page = new BufferedImage(20, 12, type);
            for (int y = 0; y < page.getHeight(); y++) {
                for (int x = 0; x < page.getWidth(); x++) {
                    page.setRGB(x, y, 0x80400000 | (x << 8) | y);
                }
            }
            BufferedImage region = page.getSubimage(7, 3, 5, 6);
            BoardScene.Pixels pixels = new BoardScene.PixelPool().capture(region, null);
            for (int y = 0; y < 6; y++) {
                for (int x = 0; x < 5; x++) {
                    int argb = page.getRGB(x + 7, y + 3);
                    assertEquals((argb << 8) | ((argb >>> 24) & 255), pixels.rgba(y * 5 + x));
                }
            }
        }
    }

    @Test
    void sharesExactArtworkAndOwnsPixelsAcrossImageAndScratchBufferChanges() {
        BoardScene.PixelPool pool = new BoardScene.PixelPool();
        BufferedImage image = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(20, 20, 0xff123456);
        BoardScene.Pixels first = pool.capture(image, null);
        BufferedImage copy = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        copy.setRGB(20, 20, 0xff123456);
        assertSame(first, pool.capture(copy, null));
        image.setRGB(20, 20, 0xff654321);
        BoardScene.Pixels changed = pool.capture(image, first);
        assertNotSame(first, changed);
        assertEquals(0x123456ff, first.rgba(20 * 84 + 20));
        assertEquals(0x654321ff, changed.rgba(20 * 84 + 20));
        assertSame(first, pool.capture(copy, null));
        pool.retain(List.of());
        assertNotSame(first, pool.capture(copy, null), "Unreferenced artwork must leave the pool");
        assertNull(pool.captureOverlay(new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB), null));
    }
}
